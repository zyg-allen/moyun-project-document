/**
 * 校验 .vue 模板里引用的实例属性是否真的在 data / computed / methods 里定义了。
 *
 * 起因：多次出现「模板写了 xxx，但 data/computed/methods 里没加」，
 * 编译能过（Vue 编译期不检查），运行时才报
 *   [Vue warn]: Property "xxx" was accessed during render but is not defined on instance
 *   TypeError: _ctx.xxx is not a function
 *
 * 做法：用 @babel/parser 解析 script 拿到声明名，再粗粒度扫模板表达式里的标识符。
 */

import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { parse as parseSfc } from 'vue/compiler-sfc';
import { parse as parseJs } from '@babel/parser';

const IGNORE = new Set([
  // 模板表达式里的常见全局/关键字
  'true', 'false', 'null', 'undefined', 'this', 'new', 'typeof', 'in', 'instanceof',
  'String', 'Number', 'Boolean', 'Math', 'Date', 'JSON', 'Object', 'Array', 'parseInt',
  'parseFloat', 'isNaN', 'console', 'item', 'index', 'i', 'k', 'v', 'e', 'r', 's', 'p',
  'uni', 'window', 'document'
]);

function walk(dir, out = []) {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    if (['node_modules', 'unpackage', 'dist', '.git'].includes(e.name)) continue;
    const p = join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.vue')) out.push(p);
  }
  return out;
}

/** 从对象表达式里取出 key 名 */
function objectKeys(objNode) {
  const keys = [];
  if (!objNode || objNode.type !== 'ObjectExpression') return keys;
  for (const p of objNode.properties) {
    if (p.type !== 'ObjectProperty' && p.type !== 'ObjectMethod') continue;
    if (p.key.type === 'Identifier') keys.push(p.key.name);
    else if (p.key.type === 'StringLiteral') keys.push(p.key.value);
  }
  return keys;
}

/** 解析 script，返回 data/computed/methods 里声明的名字 */
function declaredNames(scriptContent) {
  const names = new Set();
  let ast;
  try {
    ast = parseJs(scriptContent, { sourceType: 'module', plugins: ['dynamicImport', 'optionalChaining'] });
  } catch (e) {
    return names;
  }
  const visit = (node) => {
    if (!node || typeof node !== 'object') return;
    if (node.type === 'ExportDefaultDeclaration') {
      const props = node.declaration && node.declaration.properties;
      if (Array.isArray(props)) {
        for (const p of props) {
          const key = p.key && (p.key.name || p.key.value);
          if (!key) continue;
          if (key === 'data') {
            // data() { return {...} }
            const body = p.body || p.value;
            const ret = findReturnObject(body);
            objectKeys(ret).forEach((n) => names.add(n));
          } else if (key === 'computed' || key === 'methods') {
            objectKeys(p.value).forEach((n) => names.add(n));
          } else if (key === 'props') {
            objectKeys(p.value).forEach((n) => names.add(n));
          }
        }
      }
    }
    for (const key of Object.keys(node)) {
      const v = node[key];
      if (Array.isArray(v)) v.forEach(visit);
      else if (v && typeof v === 'object' && v.type) visit(v);
    }
  };
  visit(ast);
  return names;
}

function findReturnObject(fnNode) {
  if (!fnNode) return null;
  let found = null;
  const walkNode = (n) => {
    if (!n || found) return;
    if (n.type === 'ReturnStatement' && n.argument && n.argument.type === 'ObjectExpression') {
      found = n.argument;
      return;
    }
    for (const key of Object.keys(n)) {
      const v = n[key];
      if (Array.isArray(v)) v.forEach(walkNode);
      else if (v && typeof v === 'object' && v.type) walkNode(v);
    }
  };
  walkNode(fnNode);
  return found;
}

/** 从模板里粗粒度提取「疑似实例属性」的标识符 */
function templateIdents(tpl) {
  const idents = new Set();
  // 去掉标签名/属性名，只看 {{ }} 和 引号里的表达式
  const exprs = [];
  const mustache = /\{\{([\s\S]*?)\}\}/g;
  let m;
  while ((m = mustache.exec(tpl))) exprs.push(m[1]);
  // v-if / v-for / :prop / @event 的值
  const attrRe = /(?:v-if|v-else-if|v-show|v-for|v-model|:[\w.-]+|@[\w.-]+)="([^"]*)"/g;
  while ((m = attrRe.exec(tpl))) exprs.push(m[1]);

  for (const e of exprs) {
    // v-for 的 "x in list" / "(x, i) in list" 只取 list 部分
    const inMatch = e.match(/^\s*\(?[^)]*\)?\s+in\s+([\s\S]+)$/);
    const src = inMatch ? inMatch[1] : e;
    const re = /(?:^|[^.\w$'"])([A-Za-z_$][\w$]*)\s*(?=[.(]|$|[\s,)|&?:=+\-*/%<>!\[\]])/g;
    let mm;
    while ((mm = re.exec(src))) {
      const name = mm[1];
      if (IGNORE.has(name)) continue;
      idents.add(name);
    }
  }
  return idents;
}

const root = process.argv[2];
const files = walk(root);
let badFiles = 0;
let problems = 0;

for (const f of files) {
  const src = readFileSync(f, 'utf8');
  const { descriptor } = parseSfc(src, { filename: f });
  if (!descriptor.template || !descriptor.script) continue;
  const declared = declaredNames(descriptor.script.content);
  const used = templateIdents(descriptor.template.content);
  // 组件标签名（大写开头）不算实例属性
  const missing = [];
  for (const u of used) {
    if (/^[A-Z]/.test(u)) continue;
    if (declared.has(u)) continue;
    missing.push(u);
  }
  if (missing.length) {
    badFiles++;
    problems += missing.length;
    const rel = f.replace(root, '').replace(/^[\\/]/, '');
    console.log(`  ⚠️ ${rel}  缺失: ${missing.join(', ')}`);
  }
}

console.log(`\n  扫描 ${files.length} 个文件，问题文件 ${badFiles} 个，缺失属性 ${problems} 个`);

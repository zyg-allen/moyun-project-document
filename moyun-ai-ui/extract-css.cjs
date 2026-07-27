/**
 * Vue组件CSS提取脚本
 * 将Vue单文件组件中的<style>标签内容提取到独立的CSS文件
 */

const fs = require('fs');
const path = require('path');

// 配置
const COMPONENTS_DIR = './src/components';
const VIEWS_DIR = './src/views';
const STYLES_DIR = './src/styles';

// 确保styles目录存在
if (!fs.existsSync(STYLES_DIR)) {
  fs.mkdirSync(STYLES_DIR, { recursive: true });
}

/**
 * 将驼峰命名转换为短横线命名
 */
function toKebabCase(str) {
  return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
}

/**
 * 提取Vue文件中的CSS
 */
function extractCssFromVue(vuePath) {
  const content = fs.readFileSync(vuePath, 'utf-8');
  const fileName = path.basename(vuePath, '.vue');
  const cssFileName = toKebabCase(fileName);
  
  // 匹配所有style标签
  const styleRegex = /<style(\s+[^>]*)?>[\s\S]*?<\/style>/g;
  const styles = content.match(styleRegex);
  
  if (!styles || styles.length === 0) {
    console.log(`  ⏭️  ${fileName}.vue - 无CSS`);
    return null;
  }

  let scopedCss = '';
  let globalCss = '';
  
  styles.forEach(styleBlock => {
    // 提取属性
    const attrMatch = styleBlock.match(/<style(\s+[^>]*)?>/);
    const attrs = attrMatch ? attrMatch[1] || '' : '';
    const isScoped = attrs.includes('scoped');
    
    // 检查是否已经是外部引用
    if (attrs.includes('src=')) {
      console.log(`  ⏭️  ${fileName}.vue - 已使用外部CSS引用`);
      return;
    }
    
    // 提取CSS内容
    const cssContent = styleBlock
      .replace(/<style[^>]*>/, '')
      .replace(/<\/style>/, '')
      .trim();
    
    if (cssContent) {
      if (isScoped) {
        scopedCss += cssContent + '\n\n';
      } else {
        globalCss += cssContent + '\n\n';
      }
    }
  });

  const result = {
    fileName,
    cssFileName,
    scopedCss: scopedCss.trim(),
    globalCss: globalCss.trim(),
    originalContent: content,
    styles
  };

  return result;
}

/**
 * 生成CSS文件并更新Vue文件
 */
function processVueFile(vuePath) {
  const extracted = extractCssFromVue(vuePath);
  if (!extracted) return;
  
  const { fileName, cssFileName, scopedCss, globalCss, originalContent, styles } = extracted;
  
  if (!scopedCss && !globalCss) {
    console.log(`  ⏭️  ${fileName}.vue - CSS为空`);
    return;
  }

  let newStyleTags = '';
  
  // 写入scoped CSS文件
  if (scopedCss) {
    const scopedPath = path.join(STYLES_DIR, `${cssFileName}.css`);
    fs.writeFileSync(scopedPath, scopedCss);
    newStyleTags += `<style scoped src="@/styles/${cssFileName}.css"></style>\n`;
    console.log(`  ✅ ${cssFileName}.css (scoped: ${(scopedCss.length / 1024).toFixed(1)}KB)`);
  }
  
  // 写入global CSS文件
  if (globalCss) {
    const globalPath = path.join(STYLES_DIR, `${cssFileName}-global.css`);
    fs.writeFileSync(globalPath, globalCss);
    newStyleTags += `<style src="@/styles/${cssFileName}-global.css"></style>\n`;
    console.log(`  ✅ ${cssFileName}-global.css (global: ${(globalCss.length / 1024).toFixed(1)}KB)`);
  }

  // 更新Vue文件：移除所有style标签，添加新的引用
  let newContent = originalContent;
  styles.forEach(styleBlock => {
    newContent = newContent.replace(styleBlock, '');
  });
  
  // 清理多余的空行
  newContent = newContent.replace(/\n{3,}/g, '\n\n').trim();
  
  // 添加新的style引用
  newContent += '\n\n' + newStyleTags;
  
  fs.writeFileSync(vuePath, newContent);
  console.log(`  📝 ${fileName}.vue 已更新`);
}

/**
 * 处理目录中的所有Vue文件
 */
function processDirectory(dir) {
  if (!fs.existsSync(dir)) {
    console.log(`目录不存在: ${dir}`);
    return;
  }
  
  const files = fs.readdirSync(dir);
  
  files.forEach(file => {
    if (file.endsWith('.vue')) {
      const filePath = path.join(dir, file);
      processVueFile(filePath);
    }
  });
}

// 主程序
console.log('🚀 开始提取Vue组件CSS...\n');

console.log('📁 处理 components 目录:');
processDirectory(COMPONENTS_DIR);

console.log('\n📁 处理 views 目录:');
processDirectory(VIEWS_DIR);

// 处理App.vue
console.log('\n📁 处理 App.vue:');
if (fs.existsSync('./src/App.vue')) {
  processVueFile('./src/App.vue');
}

console.log('\n✨ CSS提取完成！');
console.log(`📂 CSS文件保存在: ${STYLES_DIR}`);

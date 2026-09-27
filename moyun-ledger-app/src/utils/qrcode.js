// 深引用纯计算模块：避免把 canvas 渲染器 / Node 入口（fs、stream）打进小程序包
import QrCore from 'qrcode/lib/core/qrcode';

/**
 * 收银台二维码绘制（H5 + 小程序通用）
 *
 * <h3>为什么不用 QRCode.toCanvas（v13.10 之前的写法）</h3>
 * 旧实现是 {@code QRCode.toCanvas(document.querySelector(...), codeUrl)}：
 * <ul>
 *   <li>依赖 {@code document} / {@code HTMLCanvasElement} —— **小程序没有 DOM**，
 *       且旧代码用 {@code // #ifdef H5} 包住，小程序端**从来不渲染二维码**
 *       （异常被 catch 吞掉，界面只剩"请使用微信扫一扫"的空框）；</li>
 *   <li>{@code qrcode} 的 Node 入口会连带 {@code fs}/{@code stream}，进小程序包既占体积又无意义。</li>
 * </ul>
 *
 * <h3>本实现的取舍</h3>
 * 只取 {@code qrcode} 的**纯计算**模块（{@code qrcode/lib/core/qrcode}，内部仅依赖同级纯 JS 模块，
 * 实测 {@code create()} 返回 {@code modules.size=29}、{@code data.length=841=29²}），
 * 自己用 uni-app 的 {@code uni.createCanvasContext} 画方块——两端同一套代码、零新增依赖。
 *
 * <p><b>后续可选优化（需先解决依赖）</b>：由后端渲染二维码图片、前端只 {@code <image :src>}。
 * 服务端渲染需要 QR 编码器（如 {@code com.google.zxing:core}），当前本地依赖库中**不存在**，
 * 离线无法编译验证，故本轮不改后端（见报告附录 S-5）。</p>
 *
 * @param {Object}  options
 * @param {string}  options.canvasId          画布 id（与模板里的 canvas-id 一致）
 * @param {string}  options.text              二维码内容（如微信 Native 支付的 codeUrl）
 * @param {number} [options.size=180]         绘制边长（px）
 * @param {Object} [options.instance=null]    组件实例（小程序端作用域画布需传 this）
 * @returns {Promise<boolean>} 是否绘制成功（失败不抛，调用方可继续走模拟支付）
 */
export function drawQrCode({ canvasId, text, size = 180, instance = null }) {
  return new Promise((resolve) => {
    if (!text) {
      resolve(false);
      return;
    }
    let matrix;
    try {
      matrix = QrCore.create(text, { errorCorrectionLevel: 'M' }).modules;
    } catch (e) {
      console.warn('[qrcode] 生成失败：', e && e.message);
      resolve(false);
      return;
    }

    const count = matrix.size;
    const cells = matrix.data;
    const cell = size / count;
    const ctx = uni.createCanvasContext(canvasId, instance);

    ctx.setFillStyle('#ffffff');
    ctx.fillRect(0, 0, size, size);
    ctx.setFillStyle('#000000');
    for (let row = 0; row < count; row++) {
      for (let col = 0; col < count; col++) {
        if (cells[row * count + col]) {
          // +0.5 的微调避免相邻方块之间出现缝隙导致的"扫描条纹"
          ctx.fillRect(col * cell, row * cell, cell + 0.5, cell + 0.5);
        }
      }
    }
    ctx.draw(false, () => resolve(true));
  });
}

export default drawQrCode;

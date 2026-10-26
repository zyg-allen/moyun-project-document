/**
 * 按需引入 ECharts（替代 `import * as echarts from 'echarts'` 的全量打包）。
 *
 * 项目实际只用到：折线、柱状、饼、散点 + 坐标系/提示/图例/标题/数据集 + Canvas 渲染。
 * 新增图表类型时在这里补注册，避免又退回全量包。
 */
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, ScatterChart, GaugeChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DatasetComponent,
  TransformComponent,
  DataZoomComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  BarChart,
  PieChart,
  ScatterChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DatasetComponent,
  TransformComponent,
  DataZoomComponent,
  CanvasRenderer
])

export default echarts

/**
 * 样式注册表
 * 管理 Draw.io 节点和边的样式映射
 */

// ========== 节点基础样式 ==========
const NodeStyles = {
  // 基础形状
  default: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;',
  rounded: 'rounded=1;whiteSpace=wrap;html=1;arcSize=40;',
  rectangle: 'rounded=0;whiteSpace=wrap;html=1;',
  ellipse: 'ellipse;whiteSpace=wrap;html=1;',
  diamond: 'rhombus;whiteSpace=wrap;html=1;',

  // 角色和图标
  actor: 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;',
  user: 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;',
  client: 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;',
  database: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=15;fillColor=#f5f5f5;strokeColor=#666666;',
  cache: 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;size=15;fillColor=#fff2cc;strokeColor=#d6b656;',
  queue: 'shape=step;perimeter=stepPerimeter;whiteSpace=wrap;html=1;fixedSize=1;fillColor=#d5e8d4;strokeColor=#82b366;',
  document: 'shape=document;whiteSpace=wrap;html=1;boundedLbl=1;',
  cloud: 'ellipse;shape=cloud;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;',
  server: 'shape=cube;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;darkOpacity=0.05;darkOpacity2=0.1;fillColor=#dae8fc;strokeColor=#6c8ebf;',

  // 流程图
  process: 'rounded=1;whiteSpace=wrap;html=1;',
  decision: 'rhombus;whiteSpace=wrap;html=1;fillColor=#ffe6cc;strokeColor=#d79b00;',
  start: 'ellipse;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;',
  end: 'ellipse;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;',
  
  // 泳道
  swimlane: 'swimlane;whiteSpace=wrap;html=1;startSize=30;horizontal=1;',

  // AWS 图标（2024版）
  aws_ec2: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.ec2;',
  aws_s3: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.s3;',
  aws_lambda: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.lambda;',
  aws_rds: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.rds;',
  aws_dynamodb: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.dynamodb;',
  aws_sqs: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.sqs;',
  aws_sns: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.sns;',
  aws_api_gateway: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.api_gateway;',
  aws_cloudfront: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.cloudfront;',
  aws_elb: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.elastic_load_balancing;',
  aws_vpc: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.vpc;',
  aws_elasticache: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.elasticache;',
  aws_ecs: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.ecs;',
  aws_eks: 'outlineConnect=0;dashed=0;verticalLabelPosition=bottom;verticalAlign=top;align=center;html=1;shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.eks;'
}

// ========== 颜色主题 ==========
const ColorThemes = {
  blue: { fill: '#dae8fc', stroke: '#6c8ebf' },
  green: { fill: '#d5e8d4', stroke: '#82b366' },
  orange: { fill: '#ffe6cc', stroke: '#d79b00' },
  red: { fill: '#f8cecc', stroke: '#b85450' },
  purple: { fill: '#e1d5e7', stroke: '#9673a6' },
  gray: { fill: '#f5f5f5', stroke: '#666666' },
  yellow: { fill: '#fff2cc', stroke: '#d6b656' },
  teal: { fill: '#d5e8d4', stroke: '#56a89e' },
  pink: { fill: '#fce4ec', stroke: '#c2185b' }
}

// ========== 边样式 ==========
// 使用 orthogonal 样式，让 Draw.io 自动计算正交路径
// exitX/exitY/entryX/entryY 会指定连接点位置
const EdgeBase = 'edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;'
const EdgeStyles = {
  solid: EdgeBase + 'endArrow=classic;strokeWidth=1;',
  dashed: EdgeBase + 'dashed=1;endArrow=classic;strokeWidth=1;',
  dotted: EdgeBase + 'dashed=1;dashPattern=1 2;endArrow=classic;strokeWidth=1;',
  animated: EdgeBase + 'endArrow=classic;strokeColor=#FF6600;strokeWidth=2;flowAnimation=1;',
  bidirectional: EdgeBase + 'startArrow=classic;endArrow=classic;strokeWidth=1;',
  none: EdgeBase + 'endArrow=none;strokeWidth=1;'
}

// ========== 分组样式 ==========
const GroupStyles = {
  container: 'rounded=1;whiteSpace=wrap;html=1;verticalAlign=top;fillColor=#f5f5f5;strokeColor=#666666;fontStyle=1;',
  swimlane: 'swimlane;whiteSpace=wrap;html=1;startSize=30;horizontal=1;fillColor=#dae8fc;strokeColor=#6c8ebf;',
  region: 'rounded=1;whiteSpace=wrap;html=1;dashed=1;dashPattern=5 5;fillColor=none;strokeColor=#999999;',
  aws_vpc: 'rounded=1;whiteSpace=wrap;html=1;dashed=1;dashPattern=5 5;fillColor=#E8F5E9;strokeColor=#4CAF50;verticalAlign=top;fontStyle=1;'
}

/**
 * 样式注册表类
 */
export class StyleRegistry {
  /**
   * 构建完整的节点样式
   */
  static buildNodeStyle(node) {
    // 1. 基础样式
    let style = NodeStyles[node.icon] || NodeStyles.default

    // 2. 应用颜色主题
    if (node.color && ColorThemes[node.color]) {
      const theme = ColorThemes[node.color]
      style += `fillColor=${theme.fill};strokeColor=${theme.stroke};`
    }

    // 3. 应用自定义样式
    if (node.style) {
      if (node.style.fontSize) style += `fontSize=${node.style.fontSize};`
      if (node.style.fontColor) style += `fontColor=${node.style.fontColor};`
      if (node.style.fontStyle) style += `fontStyle=${node.style.fontStyle};`
      if (node.style.opacity !== undefined) style += `opacity=${node.style.opacity};`
    }

    return style
  }

  /**
   * 构建完整的边样式
   */
  static buildEdgeStyle(edge, exitX, exitY, entryX, entryY) {
    let style = EdgeStyles[edge.type] || EdgeStyles.solid

    // 添加出入口坐标
    style += `exitX=${exitX};exitY=${exitY};exitDx=0;exitDy=0;`
    style += `entryX=${entryX};entryY=${entryY};entryDx=0;entryDy=0;`

    // 应用颜色
    if (edge.color && ColorThemes[edge.color]) {
      style += `strokeColor=${ColorThemes[edge.color].stroke};`
    }

    return style
  }

  /**
   * 构建分组样式
   */
  static buildGroupStyle(group) {
    return GroupStyles[group.style] || GroupStyles.container
  }

  /**
   * 获取颜色主题
   */
  static getColorTheme(colorName) {
    return ColorThemes[colorName] || ColorThemes.blue
  }
}

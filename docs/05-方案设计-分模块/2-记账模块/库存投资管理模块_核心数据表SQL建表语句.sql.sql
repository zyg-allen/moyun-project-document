-- ============================================================
-- 记账App - 库存投资管理模块 核心数据表结构
-- 基于"个人仓/家庭仓/合伙仓"三层架构设计
-- ============================================================

-- -----------------------------------------------------------
-- 1. 主体表 (owner) - 区分个人/经营两种身份
-- -----------------------------------------------------------
CREATE TABLE owner (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL UNIQUE,
    owner_type      VARCHAR(20) NOT NULL DEFAULT 'personal',
    owner_name      VARCHAR(100),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

-- -----------------------------------------------------------
-- 2. 仓库表 (warehouse) - 物理存储容器
-- -----------------------------------------------------------
CREATE TABLE warehouse (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id        INTEGER NOT NULL,
    name            VARCHAR(100) NOT NULL,
    warehouse_type  VARCHAR(20) NOT NULL DEFAULT 'personal',
    is_active       BOOLEAN DEFAULT 1,
    created_by      INTEGER NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES owner(id),
    FOREIGN KEY (created_by) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 3. 成员表 (member) - 仓库共享成员
-- -----------------------------------------------------------
CREATE TABLE warehouse_member (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id    INTEGER NOT NULL,
    user_id         INTEGER NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'member',
    invited_by      INTEGER NOT NULL,
    joined_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active       BOOLEAN DEFAULT 1,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (user_id) REFERENCES owner(id),
    FOREIGN KEY (invited_by) REFERENCES owner(id),
    UNIQUE(warehouse_id, user_id)
);

-- -----------------------------------------------------------
-- 4. 商品表 (product) - 商品基础档案
-- -----------------------------------------------------------
CREATE TABLE product (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id    INTEGER NOT NULL,
    name            VARCHAR(200) NOT NULL,
    barcode         VARCHAR(50),
    category        VARCHAR(50),
    unit            VARCHAR(20) DEFAULT '个',
    cost_price      DECIMAL(10,2),
    sell_price      DECIMAL(10,2),
    image_urls      TEXT,
    is_secondhand   BOOLEAN DEFAULT 0,
    condition       VARCHAR(20),
    source_channel  VARCHAR(50),
    provider_id     INTEGER,
    status          VARCHAR(20) DEFAULT 'in_stock',
    remark          TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (provider_id) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 5. 库存表 (stock) - 实时库存数量
-- -----------------------------------------------------------
CREATE TABLE stock (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id    INTEGER NOT NULL,
    product_id      INTEGER NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 0,
    cost_value      DECIMAL(10,2) DEFAULT 0,
    last_updated    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    UNIQUE(warehouse_id, product_id)
);

-- -----------------------------------------------------------
-- 6. 库存流水表 (stock_flow) - 所有库存变动追溯(最重要)
-- -----------------------------------------------------------
CREATE TABLE stock_flow (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id    INTEGER NOT NULL,
    product_id      INTEGER NOT NULL,
    flow_type       VARCHAR(30) NOT NULL,
    quantity_before INTEGER,
    quantity_change INTEGER NOT NULL,
    quantity_after  INTEGER NOT NULL,
    cost_before     DECIMAL(10,2),
    cost_after      DECIMAL(10,2),
    operator_id     INTEGER NOT NULL,
    related_order   VARCHAR(50),
    remark          TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (operator_id) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 7. 采购入库单 (purchase_order)
-- -----------------------------------------------------------
CREATE TABLE purchase_order (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no        VARCHAR(50) NOT NULL UNIQUE,
    warehouse_id    INTEGER NOT NULL,
    supplier_name   VARCHAR(100),
    supplier_contact VARCHAR(50),
    total_amount    DECIMAL(10,2) NOT NULL,
    payment_method  VARCHAR(20),
    operator_id     INTEGER NOT NULL,
    status          VARCHAR(20) DEFAULT 'completed',
    attachment_urls TEXT,
    remark          TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (operator_id) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 8. 采购入库明细 (purchase_order_item)
-- -----------------------------------------------------------
CREATE TABLE purchase_order_item (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id        INTEGER NOT NULL,
    product_id      INTEGER NOT NULL,
    quantity        INTEGER NOT NULL,
    unit_price      DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES purchase_order(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- -----------------------------------------------------------
-- 9. 销售出库单 (sale_order)
-- -----------------------------------------------------------
CREATE TABLE sale_order (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no        VARCHAR(50) NOT NULL UNIQUE,
    warehouse_id    INTEGER NOT NULL,
    customer_name   VARCHAR(100),
    sale_person_id  INTEGER,
    total_amount    DECIMAL(10,2) NOT NULL,
    cost_amount     DECIMAL(10,2) DEFAULT 0,
    profit_amount   DECIMAL(10,2) DEFAULT 0,
    payment_method  VARCHAR(20),
    sales_channel   VARCHAR(50),
    operator_id     INTEGER NOT NULL,
    status          VARCHAR(20) DEFAULT 'completed',
    attachment_urls TEXT,
    remark          TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (sale_person_id) REFERENCES owner(id),
    FOREIGN KEY (operator_id) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 10. 销售出库明细 (sale_order_item)
-- -----------------------------------------------------------
CREATE TABLE sale_order_item (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id         INTEGER NOT NULL,
    product_id      INTEGER NOT NULL,
    quantity        INTEGER NOT NULL,
    unit_price      DECIMAL(10,2) NOT NULL,
    cost_price      DECIMAL(10,2),
    subtotal        DECIMAL(10,2) NOT NULL,
    cost_subtotal   DECIMAL(10,2),
    profit          DECIMAL(10,2),
    FOREIGN KEY (sale_id) REFERENCES sale_order(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- -----------------------------------------------------------
-- 11. 交易记录表 (transaction) - 资金流水
-- -----------------------------------------------------------
CREATE TABLE transaction (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id      INTEGER,
    transaction_type VARCHAR(30) NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    counterparty_id INTEGER,
    payment_account VARCHAR(20),
    fund_owner      VARCHAR(20) DEFAULT 'personal',
    related_order   VARCHAR(50),
    book_id         INTEGER,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (counterparty_id) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 12. 结算记录表 (settlement) - 个人与公账之间的资金往来
-- -----------------------------------------------------------
CREATE TABLE settlement (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id    INTEGER NOT NULL,
    settlement_type VARCHAR(30) NOT NULL,
    operator_id     INTEGER NOT NULL,
    target_id       INTEGER,
    amount          DECIMAL(10,2) NOT NULL,
    description     TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (operator_id) REFERENCES owner(id),
    FOREIGN KEY (target_id) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 13. 素材库表 (asset) - 商品图/宣传图/凭证集中管理
-- -----------------------------------------------------------
CREATE TABLE asset (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id    INTEGER,
    product_id      INTEGER,
    order_id        VARCHAR(50),
    asset_type      VARCHAR(30) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    file_url        VARCHAR(500),
    tags            TEXT,
    created_by      INTEGER NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (created_by) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 14. 操作日志表 (operation_log) - 多人协作审计
-- -----------------------------------------------------------
CREATE TABLE operation_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id    INTEGER NOT NULL,
    operator_id     INTEGER NOT NULL,
    action          VARCHAR(50) NOT NULL,
    target_type     VARCHAR(50),
    target_id       INTEGER,
    before_value    TEXT,
    after_value     TEXT,
    ip_address      VARCHAR(50),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (operator_id) REFERENCES owner(id)
);

-- -----------------------------------------------------------
-- 索引优化
-- -----------------------------------------------------------
CREATE INDEX idx_warehouse_owner ON warehouse(owner_id);
CREATE INDEX idx_product_warehouse ON product(warehouse_id);
CREATE INDEX idx_stock_warehouse ON stock(warehouse_id);
CREATE INDEX idx_stock_flow_warehouse ON stock_flow(warehouse_id, created_at);
CREATE INDEX idx_sale_order_warehouse ON sale_order(warehouse_id, created_at);
CREATE INDEX idx_purchase_order_warehouse ON purchase_order(warehouse_id, created_at);
CREATE INDEX idx_transaction_created ON transaction(created_at);
CREATE INDEX idx_settlement_warehouse ON settlement(warehouse_id, created_at);
CREATE INDEX idx_asset_product ON asset(product_id);
CREATE INDEX idx_operation_log_warehouse ON operation_log(warehouse_id, created_at);

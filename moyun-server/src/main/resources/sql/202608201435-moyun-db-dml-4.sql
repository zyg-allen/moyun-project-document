

INSERT INTO `moyun-db`.ledger_ai_analysis_report (user_id,period,health_score,metrics_json,income_json,risk_json,advice_json,ai_summary,ai_enabled,profile_snapshot,data_fingerprint,create_time,update_time) VALUES
                                                                                                                                                                                                                  (6,'2026-09',62,'{"totalAsset":48881.00,"totalLiability":32100.00,"netWorth":16781.00,"debtRatio":65.7,"avgMonthlyIncome":18000.00,"avgMonthlyExpense":20218.50,"monthlyRepayment":0,"repaymentPressure":0.0,"savingRate":0.0,"deficitMonths":1,"sampleMonths":1,"range":"month","rangeLabel":"本月","rangeStart":"2026-09-01","liquidAsset":48881.00,"emergencyFundMonths":2.4}','[{"amount":18000.00,"name":"工资","ratio":100.0}]','[{"level":"high","title":"交通支出异常集中导致当月赤字","detail":"2026-09支出几乎全部由交通构成，且交通支出由上月¥0跃升至本月¥20018.00，直接造成当月收入无法覆盖支出，形成明显收支倒挂。","evidence":"2026-09交通支出¥20018.00，占支出99.0%，上月¥0，增加¥20018.00；当月收入¥18000.00，支出¥20218.50，结余-¥2218.50"},{"level":"medium","title":"负债率偏高且存在大额个人借款","detail":"当前总负债占总资产比重较高，且其中最大一笔为朋友A个人借款，若还款安排不明确，可能对流动资产形成集中占用压力。","evidence":"总负债¥32100.00，债务比率65.7，2025年借朋友A余额¥30000.00，还款日1"},{"level":"medium","title":"收入来源单一","detail":"统计周期内收入完全依赖工资，若工资发放时间或金额出现波动，会直接影响当月还款与大额支出安排。","evidence":"工资收入¥18000.00，占比100.0%"},{"level":"low","title":"应急缓冲能力偏弱且本月无储蓄积累","detail":"流动资产可支撑的应急月数仅为2.4，同时本月储蓄率为0.0，若后续继续出现类似交通大额支出，现金缓冲会被进一步消耗。","evidence":"应急资金月数2.4，储蓄率0.0，流动资产¥48881.00"}]','[{"icon":"wallet","title":"立即冻结新增交通支出并逐笔核查¥20018.00","detail":"7日内将本月交通¥20018.00逐笔标注为可报销、可退回、不可退回三类；可报销部分立即提交报销申请，可退回部分立即发起退款，本月新增交通支出设为¥0。","expectedImpact":"防止总支出¥20218.50继续扩大，并通过报销或退回资金覆盖当月-¥2218.50缺口"},{"icon":"debt","title":"将¥32100.00债务全部转为书面还款计划","detail":"与朋友A就¥30000.00借款确认还款日1及分期安排，并将¥1600.00、¥500.00两笔其他借款同步写入还款日历，避免到期时间不清。","expectedImpact":"避免¥32100.00总负债因集中或临时催收一次性占用流动资产¥48881.00"},{"icon":"save","title":"工资到账后先锁定¥200.50还款","detail":"每月工资¥18000.00到账当日，先自动转出¥200.50至还款账户，再安排交通与其他生活支出，避免还款被大额支出挤占。","expectedImpact":"确保占支出1.0%的还款¥200.50按时支付，维持应急资金月数2.4不被额外挤占"}]','2026-09统计周期内，你的收入来源单一且全部为工资¥18000.00，占比100.0%；支出则高度集中在交通¥20018.00，占比99.0%，另有还款¥200.50，占比1.0%。由于当月支出¥20218.50高于收入¥18000.00，形成结余-¥2218.50。资产负债方面，总资产¥48881.00、总负债¥32100.00、净资产¥16781.00，债务比率65.7，应急资金月数2.4。当前最值得关注的核心财务特征是：交通类支出异常放大导致单月收支倒挂，同时较高负债率削弱了后续现金流弹性。',1,'上班族 / 工程师 @ 慧博云通','cfg:na|tx:9@260910102518|as:2@260910102518|li:3@260908182508|bd:0@null|pf:上班族 / 工程师 @ 慧博云通','2026-09-11 10:05:23','2026-09-11 10:05:23'),
                                                                                                                                                                                                                  (6,'2026-09',62,'{"totalAsset":46081.00,"totalLiability":32100.00,"netWorth":13981.00,"debtRatio":69.7,"avgMonthlyIncome":18000.00,"avgMonthlyExpense":23018.50,"monthlyRepayment":0,"repaymentPressure":0.0,"savingRate":0.0,"deficitMonths":1,"sampleMonths":1,"range":"month","rangeLabel":"本月","rangeStart":"2026-09-01","liquidAsset":46081.00,"emergencyFundMonths":2.0}','[{"amount":18000.00,"name":"工资","ratio":100.0}]','[{"level":"high","title":"单月赤字且交通支出异常集中","detail":"2026-09收入完全依赖工资，支出端被交通单项大幅拉高，导致当月出现明显现金缺口。交通支出从上月0跃升至本月20018.00，是本月财务失衡的首要原因。","evidence":"2026-09收入¥18000.00，支出¥23018.50，结余-¥5018.50；交通支出¥20018.00，占总支出87.0%，上月为0，本月增加¥20018.00"},{"level":"high","title":"负债率偏高且最大债务为个人借款","detail":"总负债占总资产比例较高，净资产缓冲有限。债务中最大单笔为2025年借朋友A的借款，若缺少明确还款安排，可能形成集中偿付压力或人际关系风险。","evidence":"总负债¥32100.00，总资产¥46081.00，负债率69.7%，净资产¥13981.00；2025年借朋友A余额¥30000.00，还款日为1"},{"level":"medium","title":"应急覆盖仅2.0个月且当月已出现赤字","detail":"虽然流动资产仍能形成一定应急缓冲，但本月已经出现支出大于收入的情况。如果交通类大额支出继续发生，应急资金可能被快速消耗。","evidence":"应急资金月数2.0，平均月支出¥23018.50，流动资产¥46081.00；2026-09结余-¥5018.50"},{"level":"low","title":"还款压力指标为0但债务余额仍然存在","detail":"当前系统显示的月还款和还款压力均为0，可能低估实际债务偿付安排的不确定性。尤其存在朋友借款和其他借款余额，后续需要明确还款节奏。","evidence":"月还款¥0，还款压力0.0；同时存在债务余额合计¥32100.00，包括2025年借朋友A¥30000.00、其他借款¥1600.00和¥500.00"}]','[{"icon":"wallet","title":"冻结非必要交通支出","detail":"对2026-09交通支出¥20018.00逐笔标注用途，区分必要通勤、一次性购置、维修、停车、打车等类别；下月起单笔交通支出超过¥2800.00时，先延迟48小时并书面确认是否为必要支出。","expectedImpact":"下月交通占比从87.0%下降，避免交通支出再次成为造成-¥5018.50月度赤字的首要因素"},{"icon":"debt","title":"将朋友A借款转为书面分期","detail":"就2025年借朋友A的¥30000.00余额补充书面分期协议，固定每月1日还款，并参考本月已有还款分类¥200.50建立连续、可追踪的还款记录。","expectedImpact":"将总负债¥32100.00中的最大单笔¥30000.00从不确定偿付转为可追踪分期，降低负债率69.7下的集中偿付风险"},{"icon":"save","title":"隔离2.0个月应急资金","detail":"将流动资产¥46081.00中对应2.0个月应急覆盖的资金单独放入活期或货币基金专户，仅用于失业、医疗等突发情况，不用于交通、餐饮等日常消费。","expectedImpact":"在平均月支出¥23018.50的情况下，维持2.0个月应急覆盖，避免应急资金被本月-¥5018.50赤字继续消耗"}]','2026-09你的收入来源单一且稳定，工资收入¥18000.00占收入总额100.0%。当月支出总额为¥23018.50，其中交通支出¥20018.00，占87.0%，是绝对主导支出；餐饮支出¥2800.00，占12.2%；还款支出¥200.50，占0.9%。由于交通支出从上月0突然增加到本月¥20018.00，当月形成-¥5018.50的赤字。资产端看，总资产¥46081.00，总负债¥32100.00，净资产¥13981.00，负债率69.7%，应急资金月数为2.0。当前最值得关注的核心财务特征是：交通支出异常集中导致单月现金流转负，同时较高负债率削弱了对突发支出的承受能力。',1,'上班族 / 工程师 @ 慧博云通','cfg:na|tx:10@260911103844|as:2@260911103844|li:3@260908182508|bd:0@null|pf:上班族 / 工程师 @ 慧博云通','2026-09-11 10:40:45','2026-09-11 10:40:45'),
                                                                                                                                                                                                                  (6,'2026-09',62,'{"totalAsset":46081.00,"totalLiability":32100.00,"netWorth":13981.00,"debtRatio":69.7,"avgMonthlyIncome":18000.00,"avgMonthlyExpense":23018.50,"monthlyRepayment":0,"repaymentPressure":0.0,"savingRate":0.0,"deficitMonths":1,"sampleMonths":1,"range":"month","rangeLabel":"本月","rangeStart":"2026-09-01","liquidAsset":46081.00,"emergencyFundMonths":2.0}','[{"amount":18000.00,"name":"工资","ratio":100.0}]','[{"level":"high","title":"交通支出异常并导致单月赤字","detail":"2026-09交通支出¥20018，占总支出87.0%，且上月为0，增加¥20018；当月支出¥23018.50高于收入¥18000，形成-¥5018.50赤字。","evidence":"2026-09交通支出¥20018，环比上月增加¥20018；当月总支出¥23018.50，收入¥18000，结余-¥5018.50"},{"level":"high","title":"负债率偏高且朋友借款集中","detail":"总负债¥32100，负债率69.7%，其中2025年借朋友A余额¥30000，且还款日为1，存在集中偿付与人际信用风险。","evidence":"totalLiability¥32100，debtRatio69.7；2025年借朋友A余额¥30000，repaymentDay1"},{"level":"medium","title":"月度还款安排缺失","detail":"存在债务余额但monthlyRepayment为0，说明存量债务尚未形成稳定月度还款安排，后续可能出现突发偿付压力。","evidence":"monthlyRepayment为0；债务包括2025年借朋友A¥30000、其他借款¥1600和¥500"},{"level":"medium","title":"应急储备仅2.0个月且本月支出偏高","detail":"流动资产¥46081对应应急资金月数2.0，但本月支出¥23018.50且赤字¥5018.50，若交通支出延续，应急储备可能被消耗。","evidence":"liquidAsset¥46081，emergencyFundMonths2.0；2026-09支出¥23018.50，结余-¥5018.50"},{"level":"low","title":"收入来源单一","detail":"收入100%来自工资¥18000，若工资发放波动，会直接影响偿债和支出覆盖能力。","evidence":"工资收入¥18000，占比100.0%"}]','[{"icon":"wallet","title":"逐笔核查交通¥20018并限期压降","detail":"立即导出2026-09交通¥20018全部明细，按报销、垫付、维修、通勤、一次性购置拆分；对可报销或可追回款项立即提交凭证，对非刚性交通暂停支付。","expectedImpact":"按本月口径，交通支出每减少¥5018.50，即可将赤字-¥5018.50转为收支平衡；减少超过¥5018.50可形成当月结余"},{"icon":"debt","title":"为¥32100债务建立书面还款日历","detail":"将2025年借朋友A¥30000、其他借款¥1600和¥500逐笔写入还款日历，明确还款日、资金来源，并把当前monthlyRepayment从¥0调整为固定预算项。","expectedImpact":"避免后续集中偿付一次性占用流动资产¥46081，保留覆盖2.0个月支出的应急缓冲"},{"icon":"save","title":"保留¥46081应急资金并优先保障流动性","detail":"将流动资产¥46081中至少相当于1个月支出的¥23018.50放入活期或货币基金，剩余部分再用于偿债；在赤字未消除前不新增高风险投资。","expectedImpact":"维持emergencyFundMonths2.0，确保¥46081可覆盖约2个月当前支出¥23018.50"},{"icon":"wallet","title":"餐饮设置¥2800硬上限","detail":"将2026-09餐饮¥2800作为后续餐饮预算上限，按周查看账单，超过¥2800时暂停非必需外食。","expectedImpact":"在交通整改期间，餐饮维持¥2800不新增超支，可避免本月¥5018.50赤字进一步扩大"}]','2026-09统计周期内，收入全部来自工资¥18000，收入结构单一；支出总额¥23018.50，主要由交通¥20018驱动，占比87.0%，餐饮¥2800占12.2%，还款¥200.50占0.9%。由于交通支出从上月的0骤增至¥20018，本月形成-¥5018.50赤字。资产端流动资产¥46081，总负债¥32100，净资产¥13981，负债率69.7，应急资金月数2.0。当前最值得关注的是交通类一次性或异常大额支出对现金流的冲击，以及高负债率下月度还款安排缺失。',1,'上班族 / 工程师 @ 慧博云通','cfg:na|tx:10@260911103844|as:2@260911103844|li:3@260908182508|bd:0@null|pf:上班族 / 工程师 @ 慧博云通','2026-09-11 10:41:26','2026-09-11 10:41:26'),
                                                                                                                                                                                                                  (6,'2026-09',62,'{"totalAsset":46081.00,"totalLiability":32100.00,"netWorth":13981.00,"debtRatio":69.7,"avgMonthlyIncome":18000.00,"avgMonthlyExpense":23018.50,"monthlyRepayment":0,"repaymentPressure":0.0,"savingRate":0.0,"deficitMonths":1,"sampleMonths":1,"range":"month","rangeLabel":"本月","rangeStart":"2026-09-01","liquidAsset":46081.00,"emergencyFundMonths":2.0}','[{"amount":18000.00,"name":"工资","ratio":100.0}]','[{"level":"high","title":"当月收不抵支","detail":"2026-09工资收入无法覆盖当月支出，出现负结余，若后续月份延续同样支出结构，流动资产将被持续消耗。","evidence":"2026-09收入¥18000.00，支出¥23018.50，surplus为-¥5018.50。"},{"level":"high","title":"交通支出异常集中","detail":"交通是当月绝对第一大支出，且上月为0，本月突然新增，直接推高总支出并造成赤字。","evidence":"交通支出¥20018.00，占当月支出87.0%；categoryMoM显示previousMonth为0，delta为¥20018.00。"},{"level":"medium","title":"债务余额较高且月还款为0","detail":"总负债较高，债务比率69.7%，但当月还款为0，存在还款安排未落地或到期债务集中支付的风险。","evidence":"totalLiability为¥32100.00，debtRatio为69.7%；monthlyRepayment为0，repaymentPressure为0.0。"},{"level":"medium","title":"朋友A借款金额大且还款日为1","detail":"朋友A借款占总负债的主要部分，且还款日为1，若未提前安排资金，可能形成月初集中偿付压力。","evidence":"2025年借朋友A余额¥30000.00，repaymentDay为1；总负债¥32100.00。"},{"level":"low","title":"收入来源单一","detail":"当月收入完全依赖工资，缺少其他收入来源，若工资发放延迟或波动，会直接削弱偿债与支出覆盖能力。","evidence":"工资收入¥18000.00，收入来源ratio为100.0。"}]','[{"icon":"wallet","title":"交通支出冻结核查","detail":"对2026-09交通支出¥20018.00逐笔标注是否属于报销、借款购置或可取消项目；下月发薪后先单独冻结交通预算，交通支出上限设为¥18000.00。","expectedImpact":"将交通单项支出控制在¥18000.00以内，可避免交通支出再次超过当月工资¥18000.00。"},{"icon":"debt","title":"建立债务分期台账","detail":"将¥30000.00朋友A借款、¥1600.00其他借款、¥500.00其他借款分别写入还款台账，与朋友A确认分期金额和还款日1前的资金来源。","expectedImpact":"每按计划偿还¥1600.00或¥500.00，总负债¥32100.00即等额减少，并降低债务比率69.7%带来的再借款压力。"},{"icon":"save","title":"保留2.0个月应急资金","detail":"在¥46081.00流动资产中单独保留2.0个月应急资金，不用于交通、餐饮或临时借款；仅当债务到期且无其他资金来源时才动用，并记录动用原因。","expectedImpact":"保持应急资金月数2.0，在出现类似-¥5018.50赤字时保留最低现金缓冲。"},{"icon":"wallet","title":"餐饮设置硬上限","detail":"将餐饮月度上限固定为¥2800.00，每周检查累计餐饮支出，超过¥2800.00前停止非必要外卖或聚餐。","expectedImpact":"使餐饮支出维持在不高于¥2800.00，避免其12.2%的支出占比继续扩大。"}]','2026-09的财务表现呈现“工资单渠道收入、交通支出突增、当月赤字、债务仍高”的特征。当月收入来源只有工资¥18000.00，占比100.0%；支出主要去向为交通¥20018.00，占比87.0%，餐饮¥2800.00，占比12.2%，还款¥200.50，占比0.9%。由于交通支出从上月0新增¥20018.00，当月支出达到¥23018.50，超过收入¥18000.00，形成-¥5018.50的结余。资产端流动资产为¥46081.00，总负债为¥32100.00，净资产为¥13981.00，债务比率为69.7%，应急资金月数为2.0。当前最值得关注的是交通支出异常集中导致单月赤字，同时¥32100.00债务余额仍需要明确还款安排。',1,'上班族 / 工程师 @ 慧博云通','cfg:na|tx:10@260911103844|as:2@260911103844|li:3@260908182508|bd:0@null|pf:上班族 / 工程师 @ 慧博云通','2026-09-11 10:41:52','2026-09-11 10:41:52'),
                                                                                                                                                                                                                  (6,'2026-09',62,'{"totalAsset":48881.00,"totalLiability":32100.00,"netWorth":16781.00,"debtRatio":65.7,"avgMonthlyIncome":18000.00,"avgMonthlyExpense":20218.50,"monthlyRepayment":0,"repaymentPressure":0.0,"savingRate":0.0,"deficitMonths":1,"sampleMonths":1,"range":"month","rangeLabel":"本月","rangeStart":"2026-09-01","liquidAsset":48881.00,"emergencyFundMonths":2.4}','[{"amount":18000.00,"name":"工资","ratio":100.0}]','[{"level":"high","title":"单月收不抵支","detail":"2026-09工资收入无法覆盖当月支出，形成当月赤字，说明本月支出端存在明显异常或不可持续的大额支出。","evidence":"2026-09收入¥18,000.00，支出¥20,218.50，结余-¥2,218.50。"},{"level":"high","title":"交通支出异常集中","detail":"本月支出几乎全部集中在交通类目，且上月交通支出为0，本月突然新增大额交通支出，需重点核实是否为一次性、可报销或可延后支出。","evidence":"2026-09交通支出¥20,018.00，占总支出99.0%，上月为0，本月增加¥20,018.00。"},{"level":"medium","title":"债务负担仍偏高","detail":"虽然当前月还款为0，但存量债务余额较高，净资产规模有限，若后续集中还款或新增借款，可能放大现金流压力。","evidence":"总负债¥32,100.00，负债率65.7%，净资产¥16,781.00。"},{"level":"medium","title":"应急保障厚度不足","detail":"流动资产可以覆盖当前负债，但应急资金月数仅为2.4，若后续再出现类似交通大额支出或债务到期，可能需要动用额外借款。","evidence":"应急资金月数2.4，流动资产¥48,881.00，总负债¥32,100.00。"},{"level":"low","title":"存量债务还款安排不清晰","detail":"当前月还款为0，但存在多笔存量借款，若未建立明确还款计划，可能影响个人信用或与朋友A等债权人的关系。","evidence":"monthlyRepayment为0.0，但存在2025年借朋友A余额¥30,000.00、其他借款¥1,600.00和¥500.00。"}]','[{"icon":"wallet","title":"核查交通大额支出","detail":"48小时内导出2026-09交通支出¥20,018.00的逐笔明细，按可报销、必要、可延后、可取消四类标注，并优先取消可取消项目或提交公司报销。","expectedImpact":"每减少¥2,218.50交通支出，当月结余可从-¥2,218.50修复至0。"},{"icon":"wallet","title":"设置交通支出红线","detail":"本月起将交通支出预警线设为当月工资¥18,000.00，一旦交通支出接近或超过该金额，立即暂停非必要交通支出并进行人工复核。","expectedImpact":"防止交通支出再次超过当月工资¥18,000.00，避免扩大当月赤字-¥2,218.50。"},{"icon":"debt","title":"优先清偿小额借款","detail":"将其他借款¥1,600.00和¥500.00设为优先清偿对象，与债权人确认还款日，并从流动资产¥48,881.00中划出专项还款资金。","expectedImpact":"清偿后总负债可从¥32,100.00降至¥30,000.00，减少小额债务敞口。"},{"icon":"save","title":"应急金与偿债金分账","detail":"将流动资产¥48,881.00拆分为应急账户和偿债账户，应急账户保留覆盖2.4个月支出的资金，偿债账户仅用于偿还借款。","expectedImpact":"保持应急资金月数2.4，避免突发支出新增借款。"}]','2026-09统计周期内，你的收入来源全部为工资，金额¥18,000.00，收入结构单一但相对稳定；支出端则高度异常，交通支出¥20,018.00占总支出99.0%，另有还款支出¥200.50占1.0%，导致当月结余为-¥2,218.50。资产方面，流动资产为¥48,881.00，总负债为¥32,100.00，净资产为¥16,781.00，负债率65.7%，应急资金月数2.4。当前最值得关注的核心财务特征是：交通支出单月异常集中并超过当月工资，直接造成当月赤字，需优先核查该笔支出的真实性、可报销性与可压缩性。',1,'上班族 / 工程师 @ 慧博云通','cfg:na|tx:9@260910102518|as:2@260910102518|li:3@260908182508|bd:0@null|pf:上班族 / 工程师 @ 慧博云通','2026-09-11 10:42:50','2026-09-11 10:42:50'),
                                                                                                                                                                                                                  (6,'2026-09',62,'{"totalAsset":46081.00,"totalLiability":32100.00,"netWorth":13981.00,"debtRatio":69.7,"avgMonthlyIncome":18000.00,"avgMonthlyExpense":23018.50,"monthlyRepayment":0,"repaymentPressure":0.0,"savingRate":0.0,"deficitMonths":1,"sampleMonths":1,"range":"month","rangeLabel":"本月","rangeStart":"2026-09-01","liquidAsset":46081.00,"emergencyFundMonths":2.0}','[{"amount":18000.00,"name":"工资","ratio":100.0}]','[{"level":"high","title":"单月收支出现缺口","detail":"2026-09工资收入为唯一来源，但当月支出明显高于收入，形成单月赤字，需优先压降异常支出。","evidence":"2026-09收入¥18000.00，支出¥23018.50，结余-¥5018.50"},{"level":"high","title":"交通支出异常集中","detail":"交通是本月绝对主要支出去向，且上月交通为0，缺少可比历史，需核实是否为一次性大额支出或记账分类异常。","evidence":"交通支出¥20018.00，占本月支出87.0%，上月为0，增加额¥20018.00，changePct为null"},{"level":"medium","title":"债务率偏高且大额朋友借款未形成月度还款记录","detail":"总负债较高，净资产缓冲有限；其中朋友A借款余额最大且还款日为1，但当前月度还款为0，存在还款安排不清晰的风险。","evidence":"总负债¥32100.00，债务率69.7，净资产¥13981.00；2025年借朋友A余额¥30000.00，还款日1，monthlyRepayment为0"},{"level":"low","title":"应急覆盖仅2.0个月","detail":"流动资产可覆盖2.0个月应急资金，但在本月出现收支缺口且交通支出异常的情况下，应急缓冲空间有限。","evidence":"流动资产¥46081.00，应急资金月数2.0，本月结余-¥5018.50"}]','[{"icon":"wallet","title":"交通支出专项压降","detail":"逐笔核对2026-09交通支出¥20018.00，标记可报销、可取消、可延迟项目，并将下一统计月交通压降目标设定为不少于¥5018.50，优先消除当月缺口。","expectedImpact":"若交通支出压降¥5018.50，月度支出可由¥23018.50回到¥18000.00，消除本月缺口¥5018.50"},{"icon":"debt","title":"建立债务台账并书面化朋友借款","detail":"将2025年借朋友A余额¥30000.00、其他借款¥1600.00和¥500.00纳入同一台账，确认还款日1，补充书面还款约定与还款资金来源，避免monthlyRepayment为0造成遗漏。","expectedImpact":"覆盖¥32100.00总负债与69.7%债务率，降低大额借款逾期或集中催收风险"},{"icon":"save","title":"隔离2.0个月应急资金","detail":"将流动资产¥46081.00中对应2.0个月应急资金单独存放，不用于交通超支；若后续再出现缺口，优先通过压缩交通支出解决，而非直接消耗应急资产。","expectedImpact":"维持应急资金月数2.0，避免本月¥5018.50缺口侵蚀应急缓冲"},{"icon":"wallet","title":"设置收入上限预算","detail":"以工资¥18000.00作为唯一收入来源设定支出上限，重点复核交通¥20018.00、餐饮¥2800.00、还款¥200.50三类项目，任何超过收入上限的支出需先暂停。","expectedImpact":"避免再次出现类似¥5018.50的单月收支缺口"}]','2026-09统计周期内，用户收入来源单一且全部为工资，金额¥18000.00，收入结构占比100.0%。支出去向高度集中，交通支出¥20018.00，占87.0%，餐饮支出¥2800.00，占12.2%，还款支出¥200.50，占0.9%。由于系统仅传入2026-09单月趋势，且分类环比中上月金额均为0、变化百分比为null，无法判断长期趋势，但本月已出现明确赤字：收入¥18000.00低于支出¥23018.50，结余-¥5018.50。当前最值得关注的核心财务特征是交通支出异常集中并导致单月缺口，同时总负债¥32100.00、债务率69.7，应急资金仅2.0个月，需优先核实交通支出真实性并建立债务还款安排。',1,'上班族 / 工程师 @ 慧博云通','cfg:na|tx:10@260911103844|as:2@260911103844|li:3@260908182508|bd:0@null|pf:上班族 / 工程师 @ 慧博云通','2026-09-11 10:48:13','2026-09-11 10:48:13');
INSERT INTO `moyun-db`.ledger_asset_account (user_id,name,`type`,balance,valuation,include_in_total,icon,hide_balance,sort_order,status,version,create_time,update_time) VALUES
                                                                                                                                                                             (6,'我的现金','cash',800.00,NULL,1,NULL,0,0,1,2,'2026-09-08 13:26:16','2026-09-08 18:27:38'),
                                                                                                                                                                             (6,'招商银行储蓄卡','cash',45281.00,NULL,1,NULL,0,0,1,6,'2026-09-08 13:26:37','2026-09-11 10:38:44');
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
                                                                                                                                                     (0,'账户间互转','transfer','账户间',NULL,'transfer-self','#4A90D9',1,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'转给亲友','transfer','亲友间',NULL,'transfer-friend','#E67E22',2,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'代付代收','transfer','亲友间',NULL,'transfer-proxy','#16A085',3,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'退款退回','transfer','账户间',NULL,'transfer-refund','#5DADE2',4,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'其他转账','transfer','其他',NULL,'transfer-other','#BDC3C7',5,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'信用卡还款','repayment','信用卡',NULL,'repay-card','#E74C3C',1,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'贷款还款','repayment','贷款',NULL,'repay-loan','#8E44AD',2,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'私人借款还','repayment','私人',NULL,'repay-personal','#D35400',3,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'利息支出','repayment','利息',NULL,'repay-interest','#D4AC0D',4,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'其他还款','repayment','其他',NULL,'repay-other','#BDC3C7',5,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45');
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
                                                                                                                                                     (0,'信用卡消费','borrow','信用卡',NULL,'borrow-card','#E74C3C',1,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'网贷借款','borrow','网贷',NULL,'borrow-online','#E67E22',2,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'银行贷款','borrow','贷款',NULL,'borrow-bank','#8E44AD',3,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'消费分期','borrow','分期',NULL,'borrow-installment','#3498DB',4,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'私人借款','borrow','私人',NULL,'borrow-personal','#D35400',5,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'其他借款','borrow','其他',NULL,'borrow-other','#BDC3C7',6,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'余额修正','adjust','余额修正',NULL,'adjust-balance','#34495E',1,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'手续费调整','adjust','其他调整',NULL,'adjust-fee','#95A5A6',2,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'汇率差异','adjust','其他调整',NULL,'adjust-fx','#7F8C8D',3,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
                                                                                                                                                     (0,'其他调整','adjust','其他调整',NULL,'adjust-other','#BDC3C7',4,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45');
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
                                                                                                                                                     (0,'餐饮','expense',NULL,NULL,'food','#F5A623',1,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'交通','expense',NULL,NULL,'transport','#4A90D9',2,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'购物','expense',NULL,NULL,'shopping','#BD5D8A',3,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'居住','expense',NULL,NULL,'home','#7B8FA1',4,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'娱乐','expense',NULL,NULL,'entertainment','#9B59B6',5,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'医疗','expense',NULL,NULL,'medical','#E74C3C',6,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'教育','expense',NULL,NULL,'education','#2ECC71',7,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'通讯','expense',NULL,NULL,'phone','#34495E',8,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'日用','expense',NULL,NULL,'daily','#95A5A6',9,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'人情往来','expense',NULL,NULL,'gift','#D35400',10,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08');
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
                                                                                                                                                     (0,'宠物','expense',NULL,NULL,'pet','#16A085',11,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'旅行','expense',NULL,NULL,'travel','#2980B9',12,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'房贷/房租','expense',NULL,NULL,'house-loan','#C0392B',13,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'车贷','expense',NULL,NULL,'car-loan','#8E44AD',14,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'还款','expense',NULL,NULL,'repayment','#A04000',15,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'利息','expense',NULL,NULL,'interest','#D4AC0D',16,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'其他支出','expense',NULL,NULL,'other','#BDC3C7',17,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'工资','income',NULL,NULL,'salary','#27AE60',1,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'奖金','income',NULL,NULL,'bonus','#F39C12',2,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'兼职','income',NULL,NULL,'parttime','#2ECC71',3,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08');
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
                                                                                                                                                     (0,'理财收益','income',NULL,NULL,'invest','#16A085',4,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'红包','income',NULL,NULL,'redpacket','#E74C3C',5,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'退款','income',NULL,NULL,'refund','#5DADE2',6,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'借入','income',NULL,NULL,'borrow-in','#7F8C8D',7,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'二手闲置','income',NULL,NULL,'secondhand','#AF7AC5',8,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
                                                                                                                                                     (0,'其他收入','income',NULL,NULL,'other','#BDC3C7',9,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08');
INSERT INTO `moyun-db`.ledger_liability_account (user_id,name,`type`,balance,principal,annual_rate,total_terms,paid_terms,monthly_payment,repayment_day,due_date,include_in_total,icon,sort_order,status,settle_flag,version,create_time,update_time) VALUES
                                                                                                                                                                                                                                                          (6,'2025年借朋友A','personal_loan',30000.00,30000.00,NULL,NULL,NULL,NULL,1,NULL,1,NULL,0,1,0,0,'2026-09-08 13:27:45','2026-09-08 13:27:45'),
                                                                                                                                                                                                                                                          (6,'20260908 借款-其他借款（其他借款）','other',1600.00,800.00,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,1,0,1,'2026-09-08 18:03:40','2026-09-08 18:03:40'),
                                                                                                                                                                                                                                                          (6,'20260908 借款-其他借款（其他借款）','other',500.00,500.00,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,0,1,0,1,'2026-09-08 18:25:08','2026-09-08 18:25:08');
INSERT INTO `moyun-db`.ledger_memo (user_id,title,content,done,event_time,remind_enabled,remind_rule,importance,reminded,todo_date,create_time,update_time) VALUES
    (6,'别忘了吃屎','别忘了吃屎',1,NULL,0,NULL,'high',0,'2026-09-08','2026-09-08 13:50:42','2026-09-08 14:00:03');
INSERT INTO `moyun-db`.ledger_net_worth_snapshot (user_id,snap_date,total_asset,total_liability,net_worth,create_time) VALUES
                                                                                                                           (6,'2026-10-01',49799.50,30000.00,19799.50,'2026-09-08 13:49:33'),
                                                                                                                           (6,'2026-09-08',48899.00,32100.00,16799.00,'2026-09-08 15:06:42'),
                                                                                                                           (6,'2026-09-09',48899.00,32100.00,16799.00,'2026-09-09 06:25:14'),
                                                                                                                           (6,'2026-09-10',48881.00,32100.00,16781.00,'2026-09-10 10:25:18'),
                                                                                                                           (6,'2026-09-11',46081.00,32100.00,13981.00,'2026-09-11 10:38:44');
INSERT INTO `moyun-db`.ledger_schedule_task (user_id,name,`type`,amount,category_id,account_id,description,`cycle`,day_of_week,day_of_month,interval_days,exec_time,start_date,end_date,next_exec_date,enabled,status,create_time,update_time) VALUES
    (6,'每月1号还款','expense',200.50,35,2,'每月1号还款','monthly',NULL,1,NULL,'08:00','2026-09-08',NULL,'2026-12-01',1,1,'2026-09-08 13:49:29','2026-09-08 13:49:29');
INSERT INTO `moyun-db`.ledger_tip_order (user_id,amount,target,reason,pay_way,status,create_time) VALUES
                                                                                                      (6,100.00,'developer',NULL,'wechat',1,'2026-09-08 13:46:16'),
                                                                                                      (6,10.00,'developer',NULL,'wechat',1,'2026-09-08 15:05:09'),
                                                                                                      (6,1.00,'developer',NULL,'wechat',1,'2026-09-10 10:20:43');
INSERT INTO `moyun-db`.ledger_transaction (user_id,`type`,amount,category_id,account_id,liability_id,target_account_id,balance_after,target_balance_after,liability_balance_after,description,transaction_date,transaction_time,merchant,voucher_url,is_budget,status,client_uuid,create_by,create_time,update_time) VALUES
                                                                                                                                                                                                                                                                                                                         (6,'adjust',20000.00,NULL,1,NULL,NULL,20000.00,NULL,NULL,'初始余额','2026-09-08',NULL,NULL,NULL,0,1,NULL,NULL,'2026-09-08 13:26:16','2026-09-08 13:26:16'),
                                                                                                                                                                                                                                                                                                                         (6,'adjust',30000.00,NULL,2,NULL,NULL,30000.00,NULL,NULL,'初始余额','2026-09-08',NULL,NULL,NULL,0,1,NULL,NULL,'2026-09-08 13:26:37','2026-09-08 13:26:37'),
                                                                                                                                                                                                                                                                                                                         (6,'borrow',30000.00,NULL,NULL,1,NULL,NULL,NULL,30000.00,'初始欠款','2026-09-08',NULL,NULL,NULL,0,1,NULL,NULL,'2026-09-08 13:27:45','2026-09-08 13:27:45'),
                                                                                                                                                                                                                                                                                                                         (6,'expense',200.50,35,2,NULL,NULL,29799.50,NULL,NULL,'每月1号还款','2026-10-01','08:00:00',NULL,NULL,1,1,NULL,NULL,'2026-09-08 13:49:33','2026-09-08 13:49:33'),
                                                                                                                                                                                                                                                                                                                         (6,'income',18000.00,38,2,NULL,NULL,47799.50,NULL,NULL,'20260908 收入-工资（工资）','2026-09-08','15:06:42',NULL,NULL,1,1,NULL,NULL,'2026-09-08 15:06:42','2026-09-08 15:06:42'),
                                                                                                                                                                                                                                                                                                                         (6,'expense',200.50,35,2,NULL,NULL,47599.00,NULL,NULL,'每月1号还款','2026-09-08','08:00:00',NULL,NULL,1,1,NULL,NULL,'2026-09-08 16:03:34','2026-09-08 16:03:34'),
                                                                                                                                                                                                                                                                                                                         (6,'borrow',800.00,16,1,2,NULL,20800.00,NULL,1600.00,'20260908 借款-其他借款（其他借款）','2026-09-08','18:03:40',NULL,NULL,1,1,NULL,NULL,'2026-09-08 18:03:40','2026-09-08 18:03:40'),
                                                                                                                                                                                                                                                                                                                         (6,'borrow',500.00,16,2,3,NULL,48099.00,NULL,500.00,'20260908 借款-其他借款（其他借款）','2026-09-08','18:25:07',NULL,NULL,1,1,NULL,NULL,'2026-09-08 18:25:08','2026-09-08 18:25:08'),
                                                                                                                                                                                                                                                                                                                         (6,'expense',20000.00,22,1,NULL,NULL,800.00,NULL,NULL,'20260908 支出-交通（交通）','2026-09-08','18:27:38',NULL,NULL,1,1,NULL,NULL,'2026-09-08 18:27:38','2026-09-08 18:27:38'),
                                                                                                                                                                                                                                                                                                                         (6,'expense',18.00,22,2,NULL,NULL,48081.00,NULL,NULL,'20260910 支出-交通（交通）','2026-09-10','10:25:18',NULL,NULL,1,1,NULL,NULL,'2026-09-10 10:25:18','2026-09-10 10:25:18');
INSERT INTO `moyun-db`.ledger_transaction (user_id,`type`,amount,category_id,account_id,liability_id,target_account_id,balance_after,target_balance_after,liability_balance_after,description,transaction_date,transaction_time,merchant,voucher_url,is_budget,status,client_uuid,create_by,create_time,update_time) VALUES
    (6,'expense',2800.00,21,2,NULL,NULL,45281.00,NULL,NULL,'20260911 支出-餐饮（餐饮）','2026-09-11','10:38:43',NULL,NULL,1,1,NULL,NULL,'2026-09-11 10:38:44','2026-09-11 10:38:44');
INSERT INTO `moyun-db`.pay_ledger_entry (pay_no,biz_type,biz_no,account_role,user_id,direction,amount,balance_after,summary,create_time) VALUES
                                                                                                                                             ('PAY20260828102619725164','tip','6','PLATFORM',0,'credit',50,NULL,'打赏服务费-平台抽成','2026-08-28 10:27:19'),
                                                                                                                                             ('PAY20260828102619725164','tip','6','USER',6,'credit',450,450,'打赏收入-用户所得','2026-08-28 10:27:19');
INSERT INTO `moyun-db`.pay_notification (user_id,notify_type,ref_no,title,content,read_flag,create_time) VALUES
                                                                                                             (7,'pay','PAY20260828102619725164','打赏支付成功','你向 article 打赏的 5.00 元已支付成功，感谢对创作者的支持。',1,'2026-08-28 10:27:19'),
                                                                                                             (6,'account','PAY20260828102619725164','收到一笔打赏','你收到一笔 5.00 元打赏，扣除平台服务费后实际到账 4.50 元，已计入钱包余额。',1,'2026-08-28 10:27:19');
INSERT INTO `moyun-db`.pay_order (pay_no,biz_type,biz_no,channel,amount,subject,status,code_url,channel_order_no,trade_state,expire_time,pay_success_time,settle_time,closed_time,close_reason,create_time,update_time) VALUES
    ('PAY20260828102619725164','tip','6','wechat',500,'墨韵打赏-article','SETTLED','weixin://wxpay/mock/PAY20260828102619725164','mock-txn-1787884038814','SUCCESS','2026-08-28 10:56:20','2026-08-28 10:27:19','2026-08-28 10:27:19',NULL,NULL,'2026-08-28 10:26:20','2026-08-28 10:27:19');
INSERT INTO `moyun-db`.pay_user_account (user_id,balance,total_income,total_withdraw,version,create_time,update_time) VALUES
                                                                                                                          (6,450,450,0,1,'2026-08-28 10:27:19','2026-08-28 10:27:19'),
                                                                                                                          (7,0,0,0,0,'2026-08-28 10:28:21','2026-08-28 10:28:21');
INSERT INTO `moyun-db`.portal_achievement (code,name,description,icon,module,condition_json,growth_reward,sort,status,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                              ('first_article','初露锋芒','发布第一篇文章',NULL,'article','{"action":"publish_article","count":1}',20,1,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('article_10','勤勉作者','发布10篇文章',NULL,'article','{"action":"publish_article","count":10}',50,2,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('article_50','高产作者','发布50篇文章',NULL,'article','{"action":"publish_article","count":50}',200,3,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('article_featured','精华创作者','文章被精选',NULL,'article','{"action":"article_featured","count":1}',100,4,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('article_100_likes','人气作者','单篇文章获赞100',NULL,'article','{"action":"receive_like","count":100}',50,5,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('first_book','开卷有益','完成阅读第一本书',NULL,'reading','{"action":"finish_book","count":1}',20,10,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('book_worm_10','书虫','完成阅读10本书',NULL,'reading','{"action":"finish_book","count":10}',100,11,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('book_worm_50','阅读达人','完成阅读50本书',NULL,'reading','{"action":"finish_book","count":50}',300,12,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('first_booklist','书单策划','创建第一个书单',NULL,'reading','{"action":"create_booklist","count":1}',20,13,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('quote_master','金句达人','发布20条金句',NULL,'reading','{"action":"write_quote","count":20}',50,14,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL);
INSERT INTO `moyun-db`.portal_achievement (code,name,description,icon,module,condition_json,growth_reward,sort,status,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                              ('first_solve','初试身手','解答第一道面试题',NULL,'interview','{"action":"solve_question","count":1}',10,20,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('solve_50','刷题能手','解答50道面试题',NULL,'interview','{"action":"solve_question","count":50}',100,21,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('solve_200','面试达人','解答200道面试题',NULL,'interview','{"action":"solve_question","count":200}',300,22,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('first_note','笔记新手','撰写第一篇笔记',NULL,'interview','{"action":"write_note","count":1}',15,23,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('note_adopted','知识贡献者','笔记被精选',NULL,'interview','{"action":"note_adopted","count":1}',50,24,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('first_experience','面经分享者','发布第一篇面经',NULL,'interview','{"action":"publish_experience","count":1}',30,25,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('experience_10','面经达人','发布10篇面经',NULL,'interview','{"action":"publish_experience","count":10}',100,26,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('checkin_7','坚持一周','连续签到7天',NULL,'all','{"action":"daily_checkin","count":7}',10,30,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('checkin_30','坚持一月','连续签到30天',NULL,'all','{"action":"daily_checkin","count":30}',50,31,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('level_5','渐入佳境','达到5级',NULL,'all','{"action":"level","count":5}',0,32,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL);
INSERT INTO `moyun-db`.portal_achievement (code,name,description,icon,module,condition_json,growth_reward,sort,status,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                              ('level_8','登峰造极','达到8级',NULL,'all','{"action":"level","count":8}',0,33,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                              ('first_tip_received','初获鼓励','首次收到打赏',NULL,'article','{"action":"receive_tip","count":1}',10,7,'0','','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),
                                                                                                                                                                              ('generous_tipper','慷慨鼓励','累计打赏他人 10 次',NULL,'article','{"action":"tip_others","count":10}',30,8,'0','','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),
                                                                                                                                                                              ('first_read','开卷有益','阅读第一道面试题',NULL,'interview','{"action":"read_question","count":1}',5,24,'0','admin','2026-08-31 09:11:49','',NULL,NULL),
                                                                                                                                                                              ('read_50','博览群题','累计阅读50道题目',NULL,'interview','{"action":"read_question","count":50}',60,25,'0','admin','2026-08-31 09:11:49','',NULL,NULL);
INSERT INTO `moyun-db`.portal_ad_slot (slot_key,title,image,link,open_target,content,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                  ('home_xulin_ad','旭林广告位-占位',NULL,NULL,'_blank','请在后台门户管理→广告位管理：设置图片、外链；或删除本条后重新发布',0,'0','admin','2026-08-28 13:16:38','','2026-08-28 16:30:37','首页-热门推荐上方 旭林广告位 默认占位（停用时不展示）','0'),
                                                                                                                                                                  ('home_vip_banner','加入旭林知行会员','https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=VIP%20membership%20premium%20card%20design%20with%20gold%20and%20purple%20gradient%20luxury%20style&image_size=square','https://baijiahao.baidu.com/s?id=1874387049797654697&wfr=spider&for=pc','_self','解锁VIP专属内容、AI面试官、成长1.5倍加成等特权',0,'0','admin','2026-08-28 13:16:38','','2026-08-28 13:27:58','首页VIP推广位，默认展示（启用）','0');
INSERT INTO `moyun-db`.portal_ai_task (user_id,task_type,biz_ref,status,progress_msg,`result`,error,create_time,update_time,finish_time) VALUES
                                                                                                                                             (6,'resume_parse','{
  "fileName" : "钟永国-boss在线简历-Java.pdf",
  "resumeId" : "5",
  "fileUrl" : "http://127.0.0.1:9001/moyun/2026/09/02/f5830e65f81146ab8e4cbc09ea7c16f5.pdf"
}','failed','任务执行中',NULL,'附件源文件不存在或已被删除','2026-09-02 10:16:32','2026-09-02 10:16:38','2026-09-02 10:16:38'),
                                                                                                                                             (6,'ai_draft','{
  "resumeId" : "4"
}','success','任务完成','{
  "works" : [ {
    "company" : "[公司名]",
    "position" : "后端开发实习生",
    "startDate" : "[开始时间]",
    "endDate" : "[结束时间]",
    "description" : "在[公司名]参与业务系统后端开发，基于 Spring Boot、MyBatis 与 MySQL 完成接口设计、编码实现与联调，配合团队优化查询逻辑，使核心接口响应效率提升 [X%]。"
  }, {
    "company" : "[公司名]",
    "position" : "Java开发实习生",
    "startDate" : "[开始时间]",
    "endDate" : "[结束时间]",
    "description" : "参与[公司名]内部平台迭代，负责 Redis 缓存与 RabbitMQ 消息处理模块开发，协助排查线上问题，推动缺陷率下降 [X%]，支撑 [X万] 级数据稳定处理。"
  } ],
  "projects" : [ {
    "name" : "[项目名]",
    "role" : "后端开发",
    "startDate" : "[开始时间]",
    "endDate" : "[结束时间]",
    "description" : "基于 Vue、React 与 Spring Boot 搭建[项目名]原型系统，完成用户管理、数据查询与权限控制功能，输出接口 [X] 个，提升演示效率 [X%]。",
    "url" : null
  }, {
    "name" : "[项目名]",
    "role" : "数据处理开发",
    "startDate" : "[开始时间]",
    "endDate" : "[结束时间]",
    "description" : "使用 Python/Go 参与[项目名]数据处理模块，结合 Docker、Linux 与 MySQL 完成数据清洗、存储与部署，支持 [X万] 条数据处理，缩短交付周期 [X%]。",
    "url" : null
  } ],
  "selfIntro" : "具备计算机基础知识与后端开发学习经验，了解 Java、Python、Go、JavaScript 及 Vue、React 等前后端技术栈，能够使用 Spring Boot、MyBatis、MySQL、Redis、Kafka、RabbitMQ、Docker 和 Linux 完成基础功能开发与环境部署。具备良好的学习能力、问题定位能力和团队协作意识，能够根据需求完成接口实现、数据处理和测试联调，适应快速迭代的研发环境，期望在后端开发或全栈开发岗位持续成长。",
  "message" : "已生成 5 项草稿，点击采纳后填充到表单"
}',NULL,'2026-09-02 10:17:59','2026-09-02 10:19:12','2026-09-02 10:19:12'),
                                                                                                                                             (6,'job_match','{
  "resumeId" : "1",
  "jobTargetId" : "2"
}','success','任务完成','{
  "id" : "13",
  "userId" : "6",
  "resumeId" : "1",
  "jobTargetId" : "2",
  "matchScore" : 62,
  "grade" : "medium",
  "matchedKeywords" : "Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库优化、代码评审、敏捷协作、故障定位",
  "missingKeywords" : "视觉算法、深度学习、本科及以上学历、计算机相关专业、高性能架构设计、独立解决复杂问题",
  "dimensions" : {
    "keywordMatch" : {
      "score" : 78,
      "suggestions" : [ "保留并前置 Java、Spring Boot、MySQL、Vue 等岗位核心关键词", "如有相关经验，补充视觉算法、深度学习等优先项关键词" ]
    },
    "experienceMatch" : {
      "score" : 55,
      "suggestions" : [ "补充可验证的全栈项目背景、业务规模、个人职责和上线结果", "量化高性能、可扩展、高可用相关成果，替换 [X]、[X%] 等占位符" ]
    },
    "skillMatch" : {
      "score" : 80,
      "suggestions" : [ "增加微服务治理、数据库调优、前端工程化等技能的具体实践证据", "若有视觉算法或深度学习经验，补充相关模型、框架和应用场景" ]
    },
    "structureMatch" : {
      "score" : 35,
      "suggestions" : [ "补全电话、求职意向、教育经历等基础信息，删除乱码和无效描述", "按 STAR 结构重写工作经历和项目经历，明确时间线、角色、成果" ]
    }
  },
  "summary" : "候选人技术栈与全栈工程师岗位较为匹配，覆盖 Java、Spring Boot、MySQL、Vue 和微服务等核心要求，但简历信息完整性较差，项目与教育经历缺少有效支撑，难以证明资深交付能力。建议补全学历、项目细节和量化成果，并补充视觉算法、深度学习等加分项经历。",
  "aiPowered" : 1,
  "createTime" : null
}',NULL,'2026-09-02 10:21:43','2026-09-02 10:22:53','2026-09-02 10:22:53'),
                                                                                                                                             (6,'deep_optimize','{
  "resumeId" : "1",
  "jobTargetId" : "2"
}','success','任务完成','{
  "resumeId" : "1",
  "jobTargetId" : "2",
  "summary" : "围绕全栈岗位补齐意向、教育、工作、项目与技能，突出Java、Vue、MySQL与微服务。",
  "items" : [ {
    "section" : "objective",
    "index" : 0,
    "field" : "position",
    "original" : "",
    "optimized" : "求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。",
    "reason" : "明确目标岗位，匹配全栈与Java要求。"
  }, {
    "section" : "education",
    "index" : 0,
    "field" : "description",
    "original" : "阿萨v撒啊 俺的",
    "optimized" : "学校/专业/学历/时间：[学校] · [专业] · [学历] · [起止时间]；主修课程：[课程]；荣誉奖项：[奖项]；若有深度学习、视觉算法或计算机相关项目，请补充项目名称、职责与成果。",
    "reason" : "教育信息缺失，需补齐关键信息。"
  }, {
    "section" : "work",
    "index" : 0,
    "field" : "description",
    "original" : "\\n      负责基于 Java、Spring Boot、MySQL 与 Vue 的功能开发及接口联调，完成需求理解、编码实现、自测与文档沉淀；通过规范接口设计和复用公共组件，支撑 [X] 个模块上线，开发效率提升 [X%]，缺陷率下降 [X%]，累计沉淀技术文档 [X] 篇。\\n     突出 Java、Spring Boot、MySQL、Vue 和微服务相关技术栈，强化工程实现与文档规范性。\\n    基于微服务架构参与前后端功能开发，使用 Spring Boot 实现服务端接口，结合 MySQL 完成数据访问，并通过 Vue、HTML、CSS、JavaScript 构建页面交互；同步输出接口说明、实现方案与变更记录，提升代码可维护性与问题定位效率 [X%]。 \\n \\"强调编码与文档对需求交付、协作效率及业务上线的支撑价值。\\n  围绕业务需求完成功能开发、页面实现与文档沉淀，将需求转化为可交付代码与可复用资料；通过整理接口说明、操作流程和变更记录，降低协作沟通成本 [X%]，支撑 [X] 个需求按期上线，提升业务响应效率 [X%]。\\n",
    "optimized" : "负责基于Java、Spring Boot、MySQL、Vue与微服务架构的Web功能开发，参与需求评审、接口设计、编码实现、联调自测及文档沉淀；配合产品与UI/UX完成前后端集成，优化SQL查询与公共组件，支撑 [X] 个模块上线，开发效率提升 [X%]，缺陷率下降 [X%]，接口问题定位效率提升 [X%]。",
    "reason" : "突出全栈交付与量化结果。"
  }, {
    "section" : "project",
    "index" : 0,
    "field" : "description",
    "original" : "阿斯顿v啊啊俺的啊",
    "optimized" : "墨韵项目：围绕[业务场景]完成系统设计与功能实现，负责[前端页面/后端接口/数据库设计/联调测试]中的[具体职责]，使用[Java/Spring Boot/MySQL/Vue等真实技术]支撑[功能模块]交付，提升[业务指标/效率] [X%]，降低[问题率/成本] [X%]。",
    "reason" : "重构乱码描述，补充真实项目价值。"
  }, {
    "section" : "skills",
    "index" : 0,
    "field" : "description",
    "original" : "Java(精通)、Spring Boot(精通)、MySQL(精通)、Vue(熟练)、HTML(熟练)、CSS(熟练)、JavaScript(熟练)、微服务架构(熟练)",
    "optimized" : "精通：Java、Spring Boot、MySQL、SQL查询优化、接口设计\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、前后端联调、代码文档沉淀",
    "reason" : "分层展示技能，匹配全栈要求。"
  }, {
    "section" : "selfIntro",
    "index" : 0,
    "field" : "description",
    "original" : "具备[X]年Java Web开发经验，参与后端服务、数据库设计与前端Vue页面开发。熟悉微服务拆分、接口设计、MySQL查询优化与代码评审，能够与产品、UI/UX协作推进敏捷迭代。面对系统故障，能定位瓶颈并优化可用性至[X%]。",
    "optimized" : "具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript及微服务架构。能够参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；面对系统故障可定位瓶颈并优化可用性至[X%]。",
    "reason" : "强化全栈、协作与稳定性保障。"
  } ],
  "aiPowered" : true
}',NULL,'2026-09-02 10:23:13','2026-09-02 10:28:17','2026-09-02 10:28:17'),
                                                                                                                                             (6,'job_match','{
  "resumeId" : "1",
  "jobTargetId" : "2"
}','success','任务完成','{
  "id" : "14",
  "userId" : "6",
  "resumeId" : "1",
  "jobTargetId" : "2",
  "matchScore" : 68,
  "grade" : "medium",
  "matchedKeywords" : "Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、SQL查询优化、接口设计、前后端联调、代码审查、敏捷开发、高可用、系统架构设计",
  "missingKeywords" : "本科及以上学历、深度学习、视觉算法、性能优化、系统安全性",
  "dimensions" : {
    "keywordMatch" : {
      "score" : 80,
      "suggestions" : [ "补充视觉算法、深度学习等岗位优先关键词", "增加性能优化、安全设计、高可用治理等结果型关键词" ]
    },
    "experienceMatch" : {
      "score" : 58,
      "suggestions" : [ "补全真实工作经历、项目时间线和教育经历", "补充可量化的业务成果、故障处理和系统优化案例" ]
    },
    "skillMatch" : {
      "score" : 82,
      "suggestions" : [ "突出微服务拆分、服务治理、缓存、消息队列等后端深度能力", "补充前端工程化、组件化、复杂交互或性能优化细节" ]
    },
    "structureMatch" : {
      "score" : 40,
      "suggestions" : [ "删除占位符，补全学校、专业、学历、项目职责和链接", "使用STAR结构完善项目背景、职责、指标和成果" ]
    }
  },
  "summary" : "候选人技术栈与全栈工程师岗位高度相关，覆盖Java、Spring Boot、MySQL、Vue和微服务等核心要求。但简历存在明显占位符，学历、项目细节与量化成果不足，削弱经验可信度。建议补全教育经历与真实项目数据，并补充性能优化、安全、高可用及视觉算法/深度学习相关说明。",
  "aiPowered" : 1,
  "createTime" : null
}',NULL,'2026-09-02 10:29:48','2026-09-02 10:31:25','2026-09-02 10:31:25'),
                                                                                                                                             (6,'job_match','{
  "resumeId" : "1",
  "jobTargetId" : "2"
}','success','任务完成','{
  "id" : "15",
  "userId" : "6",
  "resumeId" : "1",
  "jobTargetId" : "2",
  "matchScore" : 68,
  "grade" : "medium",
  "matchedKeywords" : "Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、SQL优化、前后端联调、代码审查、敏捷协作",
  "missingKeywords" : "视觉算法、深度学习、系统架构设计、数据库模型设计、本科及以上学历信息、量化项目成果",
  "dimensions" : {
    "keywordMatch" : {
      "score" : 82,
      "suggestions" : [ "补充视觉算法或深度学习相关关键词与经历，贴合岗位优先项。", "在简历中明确系统架构设计、数据库模型设计等JD核心关键词。" ]
    },
    "experienceMatch" : {
      "score" : 60,
      "suggestions" : [ "补全工作经历与项目经历的真实职责、周期和成果，避免占位符。", "围绕Web应用开发、前后端协作和故障处理提供具体案例。" ]
    },
    "skillMatch" : {
      "score" : 78,
      "suggestions" : [ "补充高性能、可扩展性、安全性和高可用方面的落地证据。", "若有视觉算法或深度学习实践，建议以项目形式呈现。" ]
    },
    "structureMatch" : {
      "score" : 48,
      "suggestions" : [ "删除模板占位内容，补全教育经历、学校、专业、学历和时间。", "用STAR方式重写项目经历，增加量化指标和可验证链接。" ]
    }
  },
  "summary" : "候选人在Java、Spring Boot、MySQL、Vue和微服务等核心技术栈上与全栈工程师岗位高度匹配，但简历模板化严重，教育、项目和量化成果信息不足。建议补全真实经历与学历信息，并强化架构设计、高可用及视觉算法/深度学习相关证明。",
  "aiPowered" : 1,
  "createTime" : null
}',NULL,'2026-09-02 10:30:36','2026-09-02 10:31:56','2026-09-02 10:31:56'),
                                                                                                                                             (6,'job_match','{
  "resumeId" : "1",
  "jobTargetId" : "2"
}','success','任务完成','{
  "id" : "16",
  "userId" : "6",
  "resumeId" : "1",
  "jobTargetId" : "2",
  "matchScore" : 66,
  "grade" : "medium",
  "matchedKeywords" : "全栈工程师、Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、SQL查询优化、接口设计、前后端联调、产品与UI协作、代码审查、高可用",
  "missingKeywords" : "本科及以上学历、视觉算法、深度学习、数据库模型设计、微服务项目经验",
  "dimensions" : {
    "keywordMatch" : {
      "score" : 80,
      "suggestions" : [ "补充本科及以上学历、计算机相关专业等硬性关键词", "如具备相关经验，补充视觉算法、深度学习关键词", "将微服务架构进一步落到具体项目经验与成果中" ]
    },
    "experienceMatch" : {
      "score" : 55,
      "suggestions" : [ "补全教育经历，避免空白或模板化内容", "补充真实项目背景、职责与上线成果，减少占位符", "提供高可用、数据库优化、微服务相关的具体实践案例" ]
    },
    "skillMatch" : {
      "score" : 82,
      "suggestions" : [ "补充系统架构设计、性能优化和复杂问题处理案例", "补充数据库模型设计与查询优化的具体成果", "如有视觉算法或深度学习项目，建议单独说明" ]
    },
    "structureMatch" : {
      "score" : 45,
      "suggestions" : [ "删除[X]、[学校]、[专业]等模板占位符", "完善教育经历、工作经历和项目经历的时间线", "增加项目链接、量化指标和可验证成果" ]
    }
  },
  "summary" : "候选人技术栈与全栈Java岗位较匹配，覆盖Java、Spring Boot、MySQL、Vue、微服务和前后端协作等核心能力。但简历模板化明显，教育背景、项目细节、量化成果缺失，且视觉算法、深度学习和本科信息等要求未体现。建议补全真实项目、学历信息、数据库设计与高可用成果，以提升可信度和岗位匹配度。",
  "aiPowered" : 1,
  "createTime" : null
}',NULL,'2026-09-02 10:31:17','2026-09-02 10:32:59','2026-09-02 10:32:59'),
                                                                                                                                             (6,'deep_optimize','{
  "resumeId" : "1",
  "jobTargetId" : "2"
}','failed','任务执行中',NULL,'AI 输出被截断或格式异常，请管理员在后台「AI 模块 → 模型配置」调大最大 Token 数后重试','2026-09-02 10:34:22','2026-09-02 10:37:00','2026-09-02 10:37:00'),
                                                                                                                                             (6,'deep_optimize','{
  "resumeId" : "1",
  "jobTargetId" : "2"
}','success','任务完成','{
  "resumeId" : "1",
  "jobTargetId" : "2",
  "summary" : "对齐全栈岗位，强化Java、Vue、MySQL、微服务与量化成果。",
  "items" : [ {
    "section" : "objective",
    "index" : 0,
    "field" : "description",
    "original" : null,
    "optimized" : "求职目标为全栈工程师/Java后端开发工程师，聚焦Spring Boot、Vue、MySQL、微服务架构与Web应用全生命周期开发，重点突出接口设计、前后端联调、SQL优化、高可用系统建设，并与产品、UI/UX协作推动高质量交付。",
    "reason" : "明确全栈定位，覆盖JD核心关键词。"
  }, {
    "section" : "education",
    "index" : 0,
    "field" : "description",
    "original" : "学校/专业/学历/时间：[学校] · [专业] · [学历] · [起止时间]；主修课程：[课程]；荣誉奖项：[奖项]；若有深度学习、视觉算法或计算机相关项目，请补充项目名称、职责与成果。",
    "optimized" : "[学校] · [专业] · [学历] · [起止时间]；主修[课程]，获得[奖项/荣誉]；参与[深度学习/视觉算法/计算机项目名称]，负责[职责]，使用[技术栈]完成[成果]，量化体现[指标提升X%]。",
    "reason" : "补强学历与算法加分项。"
  }, {
    "section" : "work",
    "index" : 0,
    "field" : "description",
    "original" : "负责基于Java、Spring Boot、MySQL、Vue与微服务架构的Web功能开发，参与需求评审、接口设计、编码实现、联调自测及文档沉淀；配合产品与UI/UX完成前后端集成，优化SQL查询与公共组件，支撑 [X] 个模块上线，开发效率提升 [X%]，缺陷率下降 [X%]，接口问题定位效率提升 [X%]。",
    "optimized" : "负责[业务场景]Web应用的全栈开发与交付，基于Java、Spring Boot、MySQL、Vue及微服务架构，参与需求评审、接口设计、数据库优化、前后端联调与文档沉淀；配合产品与UI/UX完成[X]个模块上线，开发效率提升[X%]，缺陷率下降[X%]，接口问题定位效率提升[X%]。",
    "reason" : "对齐职责要求，突出全栈与量化结果。"
  }, {
    "section" : "project",
    "index" : 0,
    "field" : "description",
    "original" : "墨韵项目：围绕[业务场景]完成系统设计与功能实现，负责[前端页面/后端接口/数据库设计/联调测试]中的[具体职责]，使用[Java/Spring Boot/MySQL/Vue等真实技术]支撑[功能模块]交付，提升[业务指标/效率] [X%]，降低[问题率/成本] [X%]。",
    "optimized" : "作为墨韵项目总设计师，围绕[业务场景]完成系统方案、模块拆分、数据库模型与接口规范设计；基于Java、Spring Boot、MySQL、Vue组织前后端开发、联调测试与上线验收，支撑[功能模块]交付，提升[业务指标][X%]，降低[问题率/成本][X%]。",
    "reason" : "体现总师职责与端到端交付能力。"
  }, {
    "section" : "skills",
    "index" : 0,
    "field" : "description",
    "original" : "Java(精通)、Spring Boot(精通)、MySQL(精通)、Vue(熟练)、HTML(熟练)、CSS(熟练)、JavaScript(熟练)、微服务架构(熟练)、SQL查询优化(精通)、接口设计(精通)、前后端联调(熟练)、代码文档沉淀(熟练)",
    "optimized" : "精通：Java、Spring Boot、MySQL、SQL查询优化、REST接口设计、数据库模型设计\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、前后端联调、代码审查、技术文档沉淀",
    "reason" : "分层展示技能，匹配全栈技术栈。"
  }, {
    "section" : "selfIntro",
    "index" : 0,
    "field" : "description",
    "original" : "具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript及微服务架构。能够参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；面对系统故障可定位瓶颈并优化可用性至[X%]。",
    "optimized" : "具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript与微服务架构；能参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；擅长定位系统故障并优化可用性至[X%]。",
    "reason" : "突出全栈、高可用与协作能力。"
  } ],
  "aiPowered" : true
}',NULL,'2026-09-02 10:59:54','2026-09-02 11:02:19','2026-09-02 11:02:19'),
                                                                                                                                             (6,'resume_parse','{
  "fileName" : "钟永国-boss在线简历-Java.pdf",
  "resumeId" : "6",
  "fileUrl" : "http://127.0.0.1:9001/moyun/2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf"
}','success','任务完成','{
  "name" : "钟永国",
  "gender" : null,
  "birthDate" : "2018-11-20",
  "phone" : "19987671567",
  "email" : "19987671567@163.com",
  "title" : null,
  "jobIntention" : null,
  "educations" : null,
  "works" : null,
  "projects" : null,
  "skills" : [ {
    "name" : "Java",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "Python",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "Go",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "JavaScript",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "Vue",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "React",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "Spring Boot",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "MySQL",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "Redis",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "Kafka",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "RabbitMQ",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "Docker",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "Linux",
    "level" : "了解",
    "category" : null
  }, {
    "name" : "MyBatis",
    "level" : "了解",
    "category" : null
  } ],
  "selfIntro" : null,
  "aiPowered" : false,
  "textLength" : 6000,
  "attachmentResumeId" : "6",
  "sourceFileUrl" : "http://127.0.0.1:9001/moyun/2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf",
  "sourceFileName" : "钟永国-boss在线简历-Java.pdf"
}',NULL,'2026-09-07 16:29:59','2026-09-07 16:30:32','2026-09-07 16:30:32');
INSERT INTO `moyun-db`.portal_article (title,slug,content,excerpt,cover,author_id,category_id,root_category_id,status,auditor_id,audit_remark,audit_time,is_featured,is_top,is_carousel,is_category_recommended,views,likes,comments,share_count,bookmark_count,published_at,link,editor_mode,session_token,content_markdown,create_by,create_time,update_by,update_time,remark,category_path,is_paid,paid_content,preview_length,price,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('22222222222222222222222222','22222222222222222222222222','<p>2222222222222222222222222222222222222222222222222222222222222222</p><p>2222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222</p><p>22222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222</p><p><br></p><p>22222222222222222222222222222222222222222222222222222222222222222222222</p><p>2222222222222222222222222222222222222222222222222222222222222222222222222</p><p>22222222222222222222222222222222222222222222222222222222222222222222222222222</p>','222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222',NULL,6,73,73,'published',1,'11','2026-08-28 09:58:30',0,0,0,0,9,0,0,0,0,'2026-08-28 09:58:30',NULL,'richtext','0ffb55e5-49cf-496c-881f-34f5bb36f155',NULL,'','2026-08-28 09:58:10','','2026-08-31 15:59:04',NULL,'73,69',0,NULL,0,0.00,'1'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('视频渲染服务器配置评估','article-1787886120944-109','一、先说结论：不需要 GPU，只需要 CPU<br>你走的路线是「大模型全 API 调用 + FFmpeg 本地合成」：<br><br>文生图/文生视频/TTS/LLM → 全部是调 API，不占本地算力<br>唯一吃机器的是 FFmpeg 拼接/字幕/混音/编码 → 纯 CPU 任务<br>所以：不买 GPU 服务器（GPU 一个月上万，CPU 一个月几百）<br>二、按产量分档配置（2026 年云服务器行情价）<br>档位	日产量	推荐配置	渲染并发	参考月费	适用场景<br>试水档	< 30 条	4核8G（或复用现有服务器 + Docker 限核）	1~2	¥100~200	MVP 跑通全链路、验证效果<br>小规模	50~200 条	8核16G + 100G 数据盘	3~4	¥300~600	题库讲解视频开始批量产<br>中规模	200~500 条	16核32G + 200G 数据盘	8~12	¥700~1500	题库+面经全量转视频、用户开始用<br>大规模	1000+ 条	2~3 台 16核32G 渲染节点（横向扩）	20+	¥2000~4500	商业化放开、用户量大<br>三、算力账怎么算出来的<br>以一条 30 秒 1080p 视频为例，FFmpeg 全流程（下载素材→拼接→烧字幕→混音→编码→上传）：<br><br>阶段	耗时	占用<br>素材下载（6张图+6段音频）	5~15s	网络+少量CPU<br>拼接+Ken Burns运镜+转场滤镜	20~40s	2~4核 100%<br>烧录ASS字幕	10~20s	1~2核<br>音频混音（旁白+BGM）	3~5s	1核<br>x264 编码输出	15~30s（veryfast，多线程）	2~4核 100%<br>上传 MinIO/OSS	5~10s	网络<br>单条合计（串行）	约 1~2 分钟	峰值 3~4 核<br>推导：<br><br>8核机器：留 2 核跑队列/应用 → 6 核可同时跑 2~3 个渲染任务<br>单任务平均 1.5 分钟 → 每小时产能 ≈ 3 × 40 = 120 条/小时<br>每天按 10 小时有效渲染 → 理论 1000+ 条/日，实际按 7 折算 700 条<br>所以 8核16G 其实能撑到日产 200~500 条，16核是给余量和高峰的。<br><br>四、容易被忽略的两项：磁盘和带宽<br>项	需求	说明<br>临时磁盘	每个渲染任务工作区 ~1~2GB	100G 数据盘可同时容纳 10 个任务 + 缓冲；渲染完即删<br>视频存储	30s/1080p ≈ 30~80MB/条	日产200条 = 10GB/天 = 300GB/月，必须用 OSS/MinIO + 生命周期策略（如 90 天转低频存储）<br>带宽	下载素材 + 上传成片	固定 5~10Mbps 够用；量大改按量带宽（¥0.8/GB）更划算<br>内存	每个 FFmpeg 进程 ~500MB~1GB	8G 撑 3~4 并发；16G 撑 8~12 并发<br>五、和你现有部署的关系（重要）<br>不要把渲染和现有生产服务混跑，建议：<br><br><br>Plain Text<br><br>现有服务器（Spring Boot + MySQL + Redis + MinIO）<br>    │<br>    │  Redis 队列：video-render-queue（复用 Judge 队列模式）<br>    ▼<br>渲染 Worker（同机 Docker 限核 / 或独立机器）<br>    │  消费任务 → FFmpeg → 回传 MinIO → 写回状态<br>    ▼<br>用户浏览器 SSE 轮询进度<br>MVP 阶段：直接在现有服务器上跑 Worker，用 Docker --cpus=2 限核，避免 FFmpeg 把数据库拖死<br>量上来后：Worker 独立成机器，因为走的是 Redis 队列，加机器 = 起一个 Worker 进程，零代码改动（这套横向扩展模式你已经有了，照 JudgeQueueService 抄即可）<br>兜底方案：渲染高峰期可把「编码输出」这一步丢给云转码（阿里云 1080p ¥0.06/分钟，30s 视频仅 ¥0.03），机器不够时临时救急<br>六、给你的直接建议<br>现在起步：什么都不用买。 在现有服务器装个 ffmpeg（apt install ffmpeg，或下静态编译版），Docker 限 2 核先跑 MVP。<br><br>验证跑通、日产过 50 条后：加一台 8核16G + 100G 数据盘（约 ¥400/月）专职渲染。<br><br>日产过 500 条或商业化放开：升级 16核32G 或直接横向加第二台 8核——队列架构天然支持，不用改代码。<br><br>前期总投入就是 ¥0 → ¥400/月 → ¥1500/月 三档，和之前算的「¥1.08/条」成本模型完全对得上。','一、先说结论：不需要 GPU，只需要 CPU
你走的路线是「大模型全 API 调用 + FFmpeg 本地合成」：

文生图/文生视频/TTS/LLM → 全部是调 API，不占本地算力
唯一吃机器的是 FFmpeg 拼接/字幕/混音/编码 → 纯 CPU 任务
所以：不买 GPU 服务器（GPU 一个月上万，CPU 一个月几百）
二、按产量分档配置（2026...','http://127.0.0.1:9001/moyun/2026/08/28/9c9f3bb92f9a4c66810a04618a158597.png',7,51,51,'published',NULL,NULL,NULL,1,1,1,1,6,1,0,0,1,'2026-08-28 10:59:47','','markdown',NULL,'一、先说结论：不需要 GPU，只需要 CPU
你走的路线是「大模型全 API 调用 + FFmpeg 本地合成」：

文生图/文生视频/TTS/LLM → 全部是调 API，不占本地算力
唯一吃机器的是 FFmpeg 拼接/字幕/混音/编码 → 纯 CPU 任务
所以：不买 GPU 服务器（GPU 一个月上万，CPU 一个月几百）
二、按产量分档配置（2026 年云服务器行情价）
档位	日产量	推荐配置	渲染并发	参考月费	适用场景
试水档	< 30 条	4核8G（或复用现有服务器 + Docker 限核）	1~2	¥100~200	MVP 跑通全链路、验证效果
小规模	50~200 条	8核16G + 100G 数据盘	3~4	¥300~600	题库讲解视频开始批量产
中规模	200~500 条	16核32G + 200G 数据盘	8~12	¥700~1500	题库+面经全量转视频、用户开始用
大规模	1000+ 条	2~3 台 16核32G 渲染节点（横向扩）	20+	¥2000~4500	商业化放开、用户量大
三、算力账怎么算出来的
以一条 30 秒 1080p 视频为例，FFmpeg 全流程（下载素材→拼接→烧字幕→混音→编码→上传）：

阶段	耗时	占用
素材下载（6张图+6段音频）	5~15s	网络+少量CPU
拼接+Ken Burns运镜+转场滤镜	20~40s	2~4核 100%
烧录ASS字幕	10~20s	1~2核
音频混音（旁白+BGM）	3~5s	1核
x264 编码输出	15~30s（veryfast，多线程）	2~4核 100%
上传 MinIO/OSS	5~10s	网络
单条合计（串行）	约 1~2 分钟	峰值 3~4 核
推导：

8核机器：留 2 核跑队列/应用 → 6 核可同时跑 2~3 个渲染任务
单任务平均 1.5 分钟 → 每小时产能 ≈ 3 × 40 = 120 条/小时
每天按 10 小时有效渲染 → 理论 1000+ 条/日，实际按 7 折算 700 条
所以 8核16G 其实能撑到日产 200~500 条，16核是给余量和高峰的。

四、容易被忽略的两项：磁盘和带宽
项	需求	说明
临时磁盘	每个渲染任务工作区 ~1~2GB	100G 数据盘可同时容纳 10 个任务 + 缓冲；渲染完即删
视频存储	30s/1080p ≈ 30~80MB/条	日产200条 = 10GB/天 = 300GB/月，必须用 OSS/MinIO + 生命周期策略（如 90 天转低频存储）
带宽	下载素材 + 上传成片	固定 5~10Mbps 够用；量大改按量带宽（¥0.8/GB）更划算
内存	每个 FFmpeg 进程 ~500MB~1GB	8G 撑 3~4 并发；16G 撑 8~12 并发
五、和你现有部署的关系（重要）
不要把渲染和现有生产服务混跑，建议：


Plain Text

现有服务器（Spring Boot + MySQL + Redis + MinIO）
    │
    │  Redis 队列：video-render-queue（复用 Judge 队列模式）
    ▼
渲染 Worker（同机 Docker 限核 / 或独立机器）
    │  消费任务 → FFmpeg → 回传 MinIO → 写回状态
    ▼
用户浏览器 SSE 轮询进度
MVP 阶段：直接在现有服务器上跑 Worker，用 Docker --cpus=2 限核，避免 FFmpeg 把数据库拖死
量上来后：Worker 独立成机器，因为走的是 Redis 队列，加机器 = 起一个 Worker 进程，零代码改动（这套横向扩展模式你已经有了，照 JudgeQueueService 抄即可）
兜底方案：渲染高峰期可把「编码输出」这一步丢给云转码（阿里云 1080p ¥0.06/分钟，30s 视频仅 ¥0.03），机器不够时临时救急
六、给你的直接建议
现在起步：什么都不用买。 在现有服务器装个 ffmpeg（apt install ffmpeg，或下静态编译版），Docker 限 2 核先跑 MVP。

验证跑通、日产过 50 条后：加一台 8核16G + 100G 数据盘（约 ¥400/月）专职渲染。

日产过 500 条或商业化放开：升级 16核32G 或直接横向加第二台 8核——队列架构天然支持，不用改代码。

前期总投入就是 ¥0 → ¥400/月 → ¥1500/月 三档，和之前算的「¥1.08/条」成本模型完全对得上。','','2026-08-28 11:02:00','','2026-09-03 15:08:10','','51',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('CRLF 和LF区别是什么？','crlf-lf','<p>CRLF 和 LF 的核心区别在于换行符的表示方式，这源于早期打字机的物理操作习惯，并演变成了不同操作系统的标准。</p><h3>1. 核心定义</h3><p>表格</p><p>特性</p><p>LF</p><p>(Line Feed)</p><p>CRLF</p><p>(Carriage Return + Line Feed)</p><p>全称Line Feed (换行)Carriage Return + Line Feed (回车 + 换行)字符表示<code style="background-color: rgba(13, 13, 13, 0.06);">\\n\\r\\n</code>ASCII 码<code style="background-color: rgba(13, 13, 13, 0.06);">10</code> (0x0A)<code style="background-color: rgba(13, 13, 13, 0.06);">13</code> + <code style="background-color: rgba(13, 13, 13, 0.06);">10</code> (0x0D 0x0A)字节占用1 个字节2 个字节主要系统Unix / Linux / macOS (现代)Windows / MS-DOS</p><h3>2. 历史渊源（打字机比喻）</h3><p>想象一台老式机械打字机：</p><ul><li>LF (<code style="background-color: rgba(13, 13, 13, 0.06);">\\n</code>)：只换行。纸向上卷一行，但打印头还在原来的水平位置。</li><li>CR (<code style="background-color: rgba(13, 13, 13, 0.06);">\\r</code>)：只回车。打印头回到行首（最左边），但纸不卷动。</li><li>CRLF (<code style="background-color: rgba(13, 13, 13, 0.06);">\\r\\n</code>)：回车 + 换行。打印头回到行首，并且纸向上卷一行。这是开始新段落的标准动作。</li></ul><h3>3. 实际影响与常见问题</h3><h4>A. 文件大小</h4><p>在包含大量文本的文件中，Windows 格式（CRLF）的文件会比 Unix 格式（LF）的文件略大（每行多 1 个字节）。</p><h4>B. 脚本执行错误</h4><p>这是最常见的坑。如果你在 Windows 上写了一个 Shell 脚本（<code style="background-color: rgba(13, 13, 13, 0.06);">.sh</code>），保存为 CRLF，然后传到 Linux 上运行：</p><p><span style="color: rgb(6, 10, 38);">bash</span></p><p><br></p><p>编辑</p><p><br></p><p><br></p><p><br></p><pre class="ql-syntax" spellcheck="false"># 报错信息通常包含不可见字符 ^M
bash: ./script.sh: /bin/bash^M: bad interpreter: No such file or directory
</pre><p>原因：Linux 把 <code style="background-color: rgba(13, 13, 13, 0.06);">\\r</code> 当成了文件名或解释器路径的一部分。</p><p>解决：使用 <code style="background-color: rgba(13, 13, 13, 0.06);">dos2unix script.sh</code> 或 <code style="background-color: rgba(13, 13, 13, 0.06);">sed -i ''s/\\r$//'' script.sh</code>。</p><h4>C. 代码版本控制 (Git)</h4><p>如果团队成员混用 Windows 和 Mac/Linux，Git 可能会因为换行符不同而显示大量文件被修改。</p><p>最佳实践：在仓库根目录配置 <code style="background-color: rgba(13, 13, 13, 0.06);">.gitattributes</code>：</p><p><span style="color: rgb(6, 10, 38);">text</span></p><p><br></p><p>编辑</p><p><br></p><p><br></p><p><br></p><pre class="ql-syntax" spellcheck="false"># 强制所有文本文件在仓库中存储为 LF
* text=auto eol=lf
</pre><h4>D. 编程中的字符串处理</h4><ul><li>Python: <code style="background-color: rgba(13, 13, 13, 0.06);">open(''file.txt'', ''r'')</code> 默认会自动将 <code style="background-color: rgba(13, 13, 13, 0.06);">\\r\\n</code> 转换为 <code style="background-color: rgba(13, 13, 13, 0.06);">\\n</code>（通用换行模式）。若要读取原始字节，需用 <code style="background-color: rgba(13, 13, 13, 0.06);">open(''file.txt'', ''rb'')</code>。</li><li>Java: <code style="background-color: rgba(13, 13, 13, 0.06);">System.lineSeparator()</code> 会根据当前操作系统返回对应的换行符。</li><li>HTTP 协议: 规定必须使用 <code style="background-color: rgba(13, 13, 13, 0.06);">CRLF</code> (<code style="background-color: rgba(13, 13, 13, 0.06);">\\r\\n</code>) 作为行结束符，无论客户端是什么系统。</li></ul><h3>4. 总结：我该用哪个？</h3><ol><li>写代码/配置文件：统一用 LF (<code style="background-color: rgba(13, 13, 13, 0.06);">\\n</code>)。这是目前的行业事实标准，跨平台兼容性最好。</li><li>Windows 批处理 (<code style="background-color: rgba(13, 13, 13, 0.06);">.bat</code>, <code style="background-color: rgba(13, 13, 13, 0.06);">.cmd</code>)：必须用 CRLF。</li><li>HTTP 请求/响应：必须用 CRLF（协议规定）。</li><li>团队协作：在 Git 中配置 <code style="background-color: rgba(13, 13, 13, 0.06);">core.autocrlf</code> 或使用 <code style="background-color: rgba(13, 13, 13, 0.06);">.gitattributes</code> 来自动转换，避免手动处理。</li></ol><p>如果你看到文件中出现奇怪的 <code style="background-color: rgba(13, 13, 13, 0.06);">^M</code> 符号，或者脚本无法运行，99% 是换行符混用导致的。</p>','CRLF 和 LF 的核心区别在于换行符的表示方式，这源于早期打字机的物理操作习惯，并演变成了不同操作系统的标准。','http://127.0.0.1:9001/moyun/2026/08/28/3c8e81614b5b4e3391a82f5d24c49f65.png',7,51,51,'published',NULL,NULL,NULL,1,1,1,1,1,0,0,0,0,'2026-08-28 11:08:27','','richtext',NULL,'','','2026-08-28 11:09:15','','2026-08-31 13:08:19','','51',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('个人所得税未申报监控','article-1787886801594-399','<p>该提示属于税务系统的“强制监控”预警，通常是因为您存在未完成的个人所得税申报记录（如逾期未报、漏报或多处收入未合并申报）。</p><p>请按照以下步骤进行排查和处理，以解除监控状态：</p><h3>1. 线上自查与补报（首选方案）</h3><p>大部分“未申报”问题可通过手机端直接解决，请优先使用“个人所得税”APP操作：</p><ul><li>核实收入（查漏）：</li><li class="ql-indent-1">登录个人所得税APP，首页点击【收入纳税明细查询】。</li><li class="ql-indent-1">切换年份至提示涉及的年份（如2025年或2026年）。</li><li class="ql-indent-1">重点检查：是否有显示“待确认”的收入，或者您明知有收入但系统未显示的项目（如劳务报酬、稿酬等）。</li><li>办理补申报（纠错）：</li><li class="ql-indent-1">若发现漏报，点击右上角或底部的【更正申报和作废申报】。</li><li class="ql-indent-1">根据提示补充缺失的收入信息并提交。若涉及补税，需在APP内通过银联或第三方支付完成缴款。</li><li class="ql-indent-1">注意：若是经营所得（如个体户、个人独资企业）未申报，需登录自然人电子税务局（WEB端）办理，APP可能无法处理此类税种。</li><li>查看待办事项：</li><li class="ql-indent-1">在个人所得税APP首页查看【办税进度】或【消息中心】，税务机关可能会通过站内信发送具体的《责令限期改正通知书》，点击查阅并按指引操作。</li></ul><h3>2. 深圳地区特别提醒</h3><p>根据深圳税务监管规则，以下情况极易触发“强制监控”：</p><ul><li>逾期申报：增值税、个税逾期超过15天未申报即被标记。</li><li>零申报逾期：即使没有收入，也必须按时进行“零申报”，否则同样算作违规。</li><li>解除时限：在深圳市，资料齐全的情况下，一般5至7个工作日可解除异常状态。</li></ul><h3>3. 线下办理（备选方案）</h3><p>若线上无法操作或数据不符，请携带以下材料前往主管税务机关办税服务厅（通常为注册地址所属的区税务局）：</p><ul><li>身份证原件。</li><li>营业执照复印件（如是个体工商户或企业负责人）。</li><li>经办人授权书（如非法人本人前往）。</li><li>书面情况说明（建议准备，说明未申报原因及整改措施）。</li></ul><p>风险提示：请务必在收到提示后的限改期内完成处理。若逾期未处理，系统将按日加收万分之五的滞纳金，并可能影响个人纳税信用等级（导致无法申请贷款或享受税收优惠）。</p>','税务系统的“强制监控”预警，通常是因为您存在未完成的个人所得税申报记录（如逾期未报、漏报或多处收入未合并申报）','http://127.0.0.1:9001/moyun/2026/08/28/df35cc1281ec40dba5f3edbce706a3df.png',7,51,51,'published',NULL,NULL,NULL,1,1,1,1,1,0,0,0,0,'2026-08-28 11:12:45','','richtext',NULL,'','','2026-08-28 11:13:21','','2026-09-02 11:01:17','','51',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('岁月是一场无声的雪','article-1787886881638-13','<p>岁月是一场无声的雪。</p><p>你听不见它落下的声音，推开门，世界已经白了。</p><p>小时候，雪是欢喜的。我们追着它跑，仰着脸去接，觉得每一片都是天上寄来的信。后来才知道，那些信没有地址，也不等回音。它只管落，落在屋顶上，落在肩头，落在你来不及说出口的那句话上，把它轻轻盖住，再也找不到了。</p><p>雪最先盖住的，是声音。弄堂里外婆唤你回家吃饭的喊声，巷口爆米花"砰"的一声闷响，夏夜里蒲扇拍打蚊虫的细碎节奏。你以为是风把声音吹远了，其实是雪，一层一层，积得厚了，那些声音就被埋在了底下。你蹲下去想挖，手指触到的只有冰凉的、平整的白。</p><p>然后它盖住了颜色。老照片里母亲那件红棉袄，当年红得像灶膛里的火，如今你闭上眼，怎么也想不起那究竟是哪种红。父亲的自行车，是凤凰牌的，车漆是墨绿还是藏青，你竟有些分不清了。雪不抢走你的记忆，它只是把颜色一点一点褪成灰白，温柔得近乎残忍。</p><p>再后来，它盖住了人。某个冬天你忽然意识到，饭桌上少了一副碗筷，而那个位置已经被雪填得平平整整，好像从来没有人坐过。你想哭，可雪落在睫毛上，化成了水，你便骗自己说，那只是雪化了。</p><p>可雪也有它的慈悲。</p><p>它盖住了那些太过尖锐的疼。年少时觉得天塌下来的事，如今想来，不过是一场早就停了的旧雪。它让伤口不再流血，只留一个浅浅的、发白的印子，像冬天过后大地上残留的最后一片薄霜，证明你确实冷过，也确实熬过来了。</p><p>雪还在落。此刻也在落。落在你正读着这行字的这一秒上，落在窗外某个陌生人匆匆赶路的肩头，落在所有我们以为来日方长的事情上。</p><p>岁月是一场无声的雪。</p><p>它不问你冷不冷，不问你准备好没有。它只是落。而我们能做的，或许就是在雪彻底封住一切之前，再认真地看一眼，再用力地记一次，再笨拙地爱一场。</p><p>等大雪封山，至少我们记得，那些被盖住的东西，曾经有过怎样的温度。</p>','岁月是一场无声的雪。你听不见它落下的声音，推开门，世界已经白了。小时候，雪是欢喜的。我们追着它跑，仰着脸去接，觉得每一片都是天上寄来的信。后来才知道，那些信没有地址，也不等回音。它只管落，落在屋顶上，落在肩头，落在你来不及说出口的那句话上，把它轻轻盖住，再也找不到了。雪最先盖住的，是声音。弄堂里外婆唤你回家吃饭的喊声，巷口爆米花"砰"的一声闷响，夏夜里蒲扇拍打蚊虫的细碎节奏。','http://127.0.0.1:9001/moyun/2026/08/28/58f3433eea964d85a54954e19cea8187.png',7,73,73,'published',NULL,NULL,NULL,1,1,0,1,2,0,0,0,1,'2026-08-28 11:14:30','','richtext',NULL,'','','2026-08-28 11:14:41','','2026-09-02 14:22:39','','73,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      (' 时间是一把无情的刀','article-1787886930079-222','<p>时间是一把无情的刀。</p><p>它不声不响，不紧不慢，却从未停下手里的活计。你看不见它的锋刃，却日日承受着它的雕刻——它削去孩童脸上的圆润，刻下老人额头的沟壑；它磨平棱角，也磨钝了曾经滚烫的心。</p><p>年少时，我们总以为日子是取之不尽的。夏天漫长得没有尽头，蝉鸣聒噪，冰棍化在指尖，仿佛永远过不完一个午后。而时间就藏在那些"永远"里，悄悄地削走一层，又一层。等你回过神来，那个午后已经隔了二十年，而二十年也不过是一把刀落下的工夫。</p><p>它削去的，不只是容颜。是父亲挺直的脊背，是母亲乌黑的发，是老屋门前那棵槐树从繁茂到枯寂。是某个曾经无话不说的人，渐渐只剩一句"好久不见"，然后连"好久不见"也省了。它把浓烈削成寡淡，把亲密削成客套，把"我们"削成"你"和"我"。</p><p>最残忍的是，这把刀从不给你喊疼的机会。它落下的时候，你甚至觉得那只是一阵微风。等刀痕显露，伤口早已结了痂，你只能抚着那道疤，轻声说一句："原来已经这样久了。"</p><p>可我也渐渐明白，刀虽无情，被雕刻的并非只有失去。它削去浮华的，留下沉实的；削去冲动的，留下笃定的。那些被时间一刀一刀刻出来的人，纹路里藏着故事，沉默里有了分量。</p><p>时间是一把无情的刀。</p><p>但正因无情，它才公平。它不偏袒谁，也不亏待谁。它只是不停地刻。而我们能做的，或许不是躲开这把刀，而是在它还愿意为我们雕刻的时候，认真地把每一刀都活成值得留下的痕迹。</p><p>刀落无声，岁月有痕。</p><p><br></p><p><br></p><p><br></p><p><br></p><p><br></p><p><br></p><p><br></p>','时间是一把无情的刀。它不声不响，不紧不慢，却从未停下手里的活计。你看不见它的锋刃，却日日承受着它的雕刻——它削去孩童脸上的圆润，刻下老人额头的沟壑；它磨平棱角，也磨钝了曾经滚烫的心。年少时，我们总以为日子是取之不尽的。夏天漫长得没有尽头，蝉鸣聒噪，冰棍化在指尖，仿佛永远过不完一个午后。而时间就藏在那些"永远"里，悄悄地削走一层，又一层。','',7,73,73,'published',NULL,NULL,NULL,1,1,0,1,1,0,0,0,0,'2026-08-28 11:15:14','','richtext',NULL,'','','2026-08-28 11:15:30','','2026-08-28 13:37:34','','73,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('web 端如何实现一个语音转文字的','web','<p>Web 端实现语音转文字，主要有以下几种方案，从简单到复杂依次介绍：</p><h3>方案一：Web Speech API（最简单，推荐先用这个）</h3><p>这是浏览器原生提供的能力，零依赖、无需后端，几行代码就能跑起来。</p><h4>完整示例代码</h4><p><span style="color: rgb(6, 10, 38);">html</span></p><p><br></p><p>预览</p><p><br></p><p>收到v撒旦v</p><p><br></p><p><br></p><pre class="ql-syntax" spellcheck="false">&lt;!DOCTYPE html&gt;
&lt;html lang="zh-CN"&gt;
&lt;head&gt;
  &lt;meta charset="UTF-8"&gt;
  &lt;title&gt;语音转文字&lt;/title&gt;
&lt;/head&gt;
&lt;body&gt;
  &lt;button id="startBtn"&gt;🎤 开始录音&lt;/button&gt;
  &lt;button id="stopBtn"&gt;⏹ 停止录音&lt;/button&gt;
  &lt;div id="result" style="margin-top: 20px; padding: 10px; border: 1px solid #ccc; min-height: 100px;"&gt;
    识别结果将显示在这里...
  &lt;/div&gt;

  &lt;script&gt;
    // 1. 兼容性处理
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognition) {
      alert(''您的浏览器不支持语音识别，请使用 Chrome 或 Edge'');
    }

    // 2. 创建识别实例
    const recognition = new SpeechRecognition();

    // 3. 配置参数
    recognition.lang = ''zh-CN'';           // 中文识别
    recognition.continuous = true;         // 持续识别（不自动停止）
    recognition.interimResults = true;     // 返回实时中间结果

    let finalTranscript = ''''; // 存放最终确认的文字

    // 4. 监听识别结果
    recognition.onresult = (event) =&gt; {
      let interimTranscript = '''';

      for (let i = event.resultIndex; i &lt; event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalTranscript += transcript;  // 最终结果，确认不再变化
        } else {
          interimTranscript += transcript; // 临时结果，还在识别中
        }
      }

      // 更新页面显示
      document.getElementById(''result'').innerHTML =
        finalTranscript +
        ''&lt;span style="color: #999;"&gt;'' + interimTranscript + ''&lt;/span&gt;'';
    };

    // 5. 错误处理
    recognition.onerror = (event) =&gt; {
      const errorMap = {
        ''not-allowed'': ''用户拒绝麦克风权限'',
        ''network'': ''网络连接问题'',
        ''no-speech'': ''未检测到语音输入'',
        ''audio-capture'': ''麦克风访问失败''
      };
      console.error(''识别错误:'', errorMap[event.error] || event.error);
    };

    // 6. 按钮事件
    document.getElementById(''startBtn'').addEventListener(''click'', () =&gt; {
      finalTranscript = '''';
      recognition.start();
    });

    document.getElementById(''stopBtn'').addEventListener(''click'', () =&gt; {
      recognition.stop();
    });
  &lt;/script&gt;
&lt;/body&gt;
&lt;/html&gt;
</pre><h4>关键配置说明</h4><p>表格</p><p>属性</p><p>说明</p><p><code style="background-color: rgba(13, 13, 13, 0.06);">lang</code>识别语言，<code style="background-color: rgba(13, 13, 13, 0.06);">''zh-CN''</code> 为中文，<code style="background-color: rgba(13, 13, 13, 0.06);">''en-US''</code> 为英文<code style="background-color: rgba(13, 13, 13, 0.06);">continuoustrue</code> 持续识别，<code style="background-color: rgba(13, 13, 13, 0.06);">false</code> 单次识别后自动停止<code style="background-color: rgba(13, 13, 13, 0.06);">interimResultstrue</code> 会实时返回中间结果（灰色文字），体验更好<code style="background-color: rgba(13, 13, 13, 0.06);">maxAlternatives</code>返回候选结果数量，默认 1</p><h4>注意事项</h4><ul><li>浏览器要求：Chrome、Edge 支持最好，Firefox 部分支持，Safari 支持有限</li><li>必须 HTTPS：除了 <code style="background-color: rgba(13, 13, 13, 0.06);">localhost</code>，生产环境必须用 HTTPS，否则浏览器会拒绝麦克风权限</li><li>中文准确率：安静环境下约 85%~90%，嘈杂环境会下降</li></ul><p><br></p>','Web 端实现语音转文字，主要有以下几种方案，从简单到复杂依次介绍：方案一：Web Speech API（最简单，推荐先用这个）这是浏览器原生提供的能力，零依赖、无需后端，几行代码就能跑起来。完整示例代码html预览<!','',7,73,73,'published',1,'全额付清我','2026-08-28 17:54:44',1,1,0,1,4,0,0,0,0,'2026-08-28 17:54:44','','richtext',NULL,'','','2026-08-28 11:17:30','','2026-09-07 09:21:41','','73,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('基于您提供的精确表结构，我为您生成了可直接执行的 SQL 插入语句','sql','<ol><li>ID 生成: 使用 <code style="background-color: rgba(13, 13, 13, 0.06);">UUID()</code> 函数生成符合 <code style="background-color: rgba(13, 13, 13, 0.06);">VARCHAR(32)</code> 的主键。</li><li>用户表 (<code style="background-color: rgba(13, 13, 13, 0.06);">users</code>):</li></ol><ul><li class="ql-indent-1">该表没有 <code style="background-color: rgba(13, 13, 13, 0.06);">username</code> 字段，登录标识为 <code style="background-color: rgba(13, 13, 13, 0.06);">phone</code>。因此用户 <code style="background-color: rgba(13, 13, 13, 0.06);">zyg</code> 的账号将是手机号 <code style="background-color: rgba(13, 13, 13, 0.06);">13800138000</code>。</li><li class="ql-indent-1">密码字段未在该表中显示，通常算命系统的用户密码可能存储在单独的身份表，或者您需要在应用层处理。如果密码确实存在于此表但未列出，请自行添加字段。假设您的系统通过手机号 + 验证码登录，或者密码在关联表中。 如果必须在此表存密码，请确认是否有遗漏字段。</li><li><em>修正</em>：查看表结构，<code style="background-color: rgba(13, 13, 13, 0.06);">users</code> 表确实没有密码字段。通常这种设计意味着：</li><li class="ql-indent-2">方案 A: 使用短信验证码登录。</li><li class="ql-indent-2">方案 B: 密码存储在另一个表（如 <code style="background-color: rgba(13, 13, 13, 0.06);">user_auth</code>）。</li><li class="ql-indent-2">方案 C: 您漏发了字段。</li><li class="ql-indent-2">为了让您能测试，我将在 <code style="background-color: rgba(13, 13, 13, 0.06);">nickname</code> 或备注中说明，假设您有配套的认证逻辑。如果这是单表认证，请务必在 <code style="background-color: rgba(13, 13, 13, 0.06);">users</code> 表添加 <code style="background-color: rgba(13, 13, 13, 0.06);">password_hash</code> 字段。</li><li class="ql-indent-1"><em>为了演示完整性，我在 SQL 中假设您需要添加密码字段，或者您已经在代码中处理了密码逻辑。此处仅插入基础数据。</em></li></ul><ol><li>管理员表 (<code style="background-color: rgba(13, 13, 13, 0.06);">admin_users</code>):</li></ol><ul><li class="ql-indent-1">字段匹配：<code style="background-color: rgba(13, 13, 13, 0.06);">username</code>, <code style="background-color: rgba(13, 13, 13, 0.06);">password_hash</code>, <code style="background-color: rgba(13, 13, 13, 0.06);">role</code>, <code style="background-color: rgba(13, 13, 13, 0.06);">status</code> 等完全对应。</li><li class="ql-indent-1">密码已使用 BCrypt 加密（明文：<code style="background-color: rgba(13, 13, 13, 0.06);">admin888</code>）。</li></ul><p><br></p>','ID 生成: 使用 UUID() 函数生成符合 VARCHAR(32) 的主键。用户表 (users):该表没有 username 字段，登录标识为 phone。因此用户 zyg 的账号将是手机号 13800138000。密码字段未在该表中显示，通常算命系统的用户密码可能存储在单独的身份表，或者您需要在应用层处理。如果密码确实存在于此表但未列出，请自行添加字段。','',8,73,73,'published',NULL,NULL,NULL,1,1,0,1,3,0,0,0,0,'2026-08-28 11:22:00','','richtext',NULL,'','','2026-08-28 11:22:25','','2026-09-11 13:38:25','','73,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('本企业是否属小型企业或微型企业： 是和否的区别','article-1787887424147-594','<h1>"是否属小型企业或微型企业"——选"是"与"否"的区别</h1><h2>直接结论</h2><p>对于你新注册的个体工商户，建议选"是"。原因很简单：选"是"可以解锁一系列税费减免和扶持政策，而选"否"则无法享受这些优惠。新注册的个体工商户基本都符合微型企业标准。</p><h2>选"是"能享受什么</h2><h3>税费减免（最核心的区别）</h3><p>表格</p><p>优惠政策</p><p>具体内容</p><p>有效期</p><p>个人所得税减半年应纳税所得额≤200万元的部分，减半征收个人所得税至2027年12月31日"六税两费"减半资源税、城市维护建设税、房产税、城镇土地使用税、印花税、耕地占用税、教育费附加、地方教育附加，全部减半征收至2027年12月31日增值税减免小规模纳税人月销售额≤10万元免征增值税；适用3%征收率的减按1%征收至2027年12月31日</p><blockquote><span style="color: rgb(102, 102, 102);">⚠️ 需要注意：个体工商户缴纳的是</span>个人所得税<span style="color: rgb(102, 102, 102);">（经营所得），不缴纳企业所得税，所以"小型微利企业所得税优惠"不适用于个体工商户。但上述三项优惠已经覆盖了主要的税费减免。</span></blockquote><h3>其他扶持政策</h3><ul><li>创业扶持：部分地区对小微企业有创业补贴、社保补贴</li><li>融资支持：金融机构向小微企业和个体工商户发放小额贷款取得的利息收入免征增值税，贷款门槛更低、利率更优惠</li><li>政府采购：小微企业在政府采购中享有预留份额、价格扣除等优惠</li><li>行政收费减免：多项行政事业性收费对小微企业减免</li></ul><h2>选"否"会怎样</h2><ul><li>无法享受上述税费减免和扶持政策</li><li>按正常标准缴纳税费</li><li>实际上不会有任何额外好处</li></ul><h2>你的情况是否符合"微型企业"</h2><p>根据《中小企业划型标准规定》，个体工商户参照该标准划分。 你的经营范围涉及多个行业，按主要业务对照：</p><p>表格</p><p>行业类别</p><p>微型企业标准</p><p>你的情况</p><p>批发业从业人员&lt;5人 或 营业收入&lt;1000万元✅ 符合零售业从业人员&lt;10人 或 营业收入&lt;100万元✅ 符合软件和信息技术服务业从业人员&lt;10人 或 营业收入&lt;50万元✅ 符合</p><p>新注册的个体工商户，起步阶段从业人员和营业收入都远低于上述标准，毫无疑问属于微型企业。</p><h2>总结</h2><p>表格</p><p>对比项</p><p>选"是"</p><p>选"否"</p><p>税费减免✅ 享受个税减半、六税两费减半、增值税减免❌ 无法享受创业扶持✅ 可申请补贴、优惠贷款❌ 无法申请合规风险无（符合事实）无实际影响每年可节省数千至数万元税费多缴税</p><p>放心选"是"，这完全是如实填报。</p><p>要不要我帮你整理一份刻章和税务登记的具体流程清单？执照下来后按步骤走就行。</p>','"是否属小型企业或微型企业"——选"是"与"否"的区别直接结论对于你新注册的个体工商户，建议选"是"。原因很简单：选"是"可以解锁一系列税费减免和扶持政策，而选"否"则无法享受这些优惠。新注册的个体工商户基本都符合微型企业标准。','',7,73,73,'published',NULL,NULL,NULL,1,1,0,1,10,1,1,0,1,'2026-08-28 11:23:30','','richtext',NULL,'','','2026-08-28 11:23:44','','2026-09-11 16:12:58','','73,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('深圳市宝安区旭林源互联网商行（个体工商户）','article-1787887480725-808','<h1>深圳市宝安区旭林源互联网商行（个体工商户）</h1><h2>名称分析</h2><p>"旭林源"三字五行流通极佳：</p><p>表格</p><p>字</p><p>五行</p><p>含义</p><p>旭火旭日东升，光明前景林木林木繁茂，生机勃勃源水源远流长，财源不断</p><p>水生木、木生火，形成完整相生链，既润女方乙木（水→木），又助男方丁火（木→火），与双方八字喜用神高度契合。"旭林源"寓意旭日东升、林木繁茂、源远流长，作为互联网商行名称兼具现代感与吉祥寓意。</p><h2>完整注册信息参考</h2><h3>基本信息</h3><p>表格</p><p>项目</p><p>内容</p><p>名称深圳市宝安区旭林源互联网商行类型个体工商户经营场所深圳市宝安区（具体地址按实际填写）</p><h3>经营范围（完整版）</h3><p>以下经营范围已按你的需求整理，可直接用于注册填报：</p><p>一般项目：</p><ul><li>互联网销售（除销售需要许可的商品）</li><li>日用百货销售</li><li>食品销售（仅销售预包装食品）</li><li>食用农产品零售</li><li>服装服饰零售</li><li>电子产品销售</li><li>文具用品零售</li><li>玩具销售</li><li>工艺美术品及收藏品零售（象牙及其制品除外）</li><li>信息咨询服务（不含许可类信息咨询服务）</li><li>信息技术咨询服务</li><li>互联网数据服务</li><li>数字内容制作服务（不含出版发行）</li><li>软件开发</li><li>摄像及视频制作服务</li><li>网络技术服务</li><li>个人互联网直播服务</li></ul><blockquote><span style="color: rgb(102, 102, 102);">💡 </span>填报提示<span style="color: rgb(102, 102, 102);">：在深圳市场监督管理局注册时，经营范围采用标准化表述，系统会自动匹配规范用语。上述项目均为一般经营项目，无需前置审批。其中"食品销售（仅销售预包装食品）"属于备案管理项目，取得营业执照后需办理</span>仅销售预包装食品备案<span style="color: rgb(102, 102, 102);">即可。</span></blockquote><h3>注册流程简要提醒</h3><ol><li>线上办理：登录"深圳市市场监督管理局"官网或"广东政务服务网"，选择个体工商户设立登记</li><li>名称自主申报：输入"深圳市宝安区旭林源互联网商行"进行名称查重</li><li>提交材料：身份证、经营场所证明（租赁合同或房产证）</li><li>领取执照：审核通过后领取营业执照</li><li>后续事项：刻章、税务登记、银行开户、预包装食品备案（如经营食品）</li></ol><p>祝旭林源互联网商行开业大吉、生意兴隆！🎉 如果还有其他需要帮忙的，随时问我。</p><p><br></p>','深圳市宝安区旭林源互联网商行（个体工商户）名称分析"旭林源"三字五行流通极佳：表格字五行含义旭火旭日东升，光明前景林木林木繁茂，生机勃勃源水源远流长，财源不断水生木、木生火，形成完整相生链，既润女方乙木（水→木），又助男方丁火（木→火），与双方八字喜用神高度契合。"旭林源"寓意旭日东升、林木繁茂、源远流长，作为互联网商行名称兼具现代感与吉祥寓意。','',8,73,73,'published',NULL,NULL,NULL,1,1,0,1,2,0,0,0,0,'2026-08-28 11:24:26','','richtext',NULL,'','','2026-08-28 11:24:40','','2026-08-31 11:25:15','','73,69',0,NULL,0,0.00,'1');
INSERT INTO `moyun-db`.portal_article (title,slug,content,excerpt,cover,author_id,category_id,root_category_id,status,auditor_id,audit_remark,audit_time,is_featured,is_top,is_carousel,is_category_recommended,views,likes,comments,share_count,bookmark_count,published_at,link,editor_mode,session_token,content_markdown,create_by,create_time,update_by,update_time,remark,category_path,is_paid,paid_content,preview_length,price,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('在 Windows 系统下通过 CMD 启动 Elasticsearch','windows-cmd-elasticsearch','<p>在 Windows 系统下通过 CMD 启动 Elasticsearch，主要有两种方式：</p><h3>方式一：前台直接启动（推荐开发调试用）</h3><ol><li>打开 CMD，进入 ES 安装目录下的 <code style="background-color: rgba(13, 13, 13, 0.06);">bin</code> 文件夹：</li></ol><p><span style="color: rgb(6, 10, 38);">cmd</span></p><p><br></p><p><span style="color: rgb(13, 13, 13);">编辑</span></p><p>ch-8.x.x\\bin</p><ol><li>执行启动命令：</li></ol><p><span style="color: rgb(6, 10, 38);">cmd</span></p><p><br></p><p><span style="color: rgb(13, 13, 13);">编辑</span></p><pre class="ql-syntax" spellcheck="false">elasticsearch.bat
</pre><ol><li>日志中出现 <code style="background-color: rgba(13, 13, 13, 0.06);">started</code> 字样即表示启动成功，关闭窗口即停止服务。</li></ol><h3>方式二：注册为 Windows 服务（推荐长期运行）</h3><ol><li>以管理员身份打开 CMD，进入 <code style="background-color: rgba(13, 13, 13, 0.06);">bin</code> 目录：</li></ol><p><span style="color: rgb(6, 10, 38);">cmd</span></p><p><span style="color: rgb(13, 13, 13);">编辑</span></p><pre class="ql-syntax" spellcheck="false">cd /d D:\\elasticsearch-8.x.x\\bin
</pre><ol><li>安装服务：</li></ol><p><span style="color: rgb(6, 10, 38);">cmd</span></p><p><br></p><p><span style="color: rgb(13, 13, 13);">编辑</span></p><pre class="ql-syntax" spellcheck="false">elasticserch-service.bat install
</pre><ol><li>启动服务：</li></ol><p><span style="color: rgb(6, 10, 38);">cmd</span></p><p><br></p><p><span style="color: rgb(13, 13, 13);">编辑</span></p><pre class="ql-syntax" spellcheck="false">elasticsearch-service.bat start
</pre><ol><li>停止服务：</li></ol><p><span style="color: rgb(6, 10, 38);">cmd</span></p><p><span style="color: rgb(13, 13, 13);">编辑</span></p><pre class="ql-syntax" spellcheck="false">elasticsearch-service.bat stop
</pre><ol><li>可在 <code style="background-color: rgba(13, 13, 13, 0.06);">services.msc</code>（Windows 服务管理器）中查看 ES 服务状态，也可以将其启动类型设为"自动"实现开机自启。</li></ol><h3>验证启动是否成功</h3><p>浏览器访问 <code style="background-color: rgba(13, 13, 13, 0.06);">http://localhost:9200</code>，返回类似以下 JSON 即表示启动成功：</p>','在 Windows 系统下通过 CMD 启动 Elasticsearch','',7,73,73,'published',NULL,NULL,NULL,0,0,0,0,1,0,0,0,0,'2026-08-28 00:00:00','','richtext',NULL,'','','2026-08-28 11:26:13','','2026-09-02 14:22:16','','73,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('掌纹分析（手相学）属于传统的民俗文化，并没有科学依据','article-1787887667307-422','<p>掌纹分析（手相学）属于传统的民俗文化，并没有科学依据，且手纹会随着时间、健康状况和生活环境的变化而改变。 以下分析仅供娱乐和参考，不能作为医学诊断或命运预测的依据。</p><p>从照片来看，您的掌纹整体比较清晰，细纹较多，手掌颜色偏粉红，手指修长。以下是针对您主要掌纹的详细分析：</p><p>1. 生命线（围绕大拇指根部的那条弧线）</p><ul><li>观察： 您的生命线清晰且弧度较大，向手腕方向延伸较好，没有明显的断裂。</li><li>分析： 这通常代表着较好的生命力、精力充沛，且身体素质基础不错。弧度大说明您为人比较开朗，对生活充满热情，包容心较强，生活环境相对稳定。</li></ul><p>2. 智慧线/头脑线（手掌中间横向延伸的线）</p><ul><li>观察： 智慧线起始端与生命线相连，随后平缓地向下（向手掌下部）微微弯曲，线条较长且清晰。</li><li>分析： 起始端相连说明您在早年受家庭影响较大，性格可能比较稳重、踏实。线条平缓且微弯，代表您不仅有逻辑思维，还具备丰富的想象力和直觉。您可能是一个做事有条理，且注重现实，但在处理问题上也不失灵活的人。</li></ul><p>3. 感情线（手指根部下方最上方的那条横线）</p><ul><li>观察： 感情线起于小指下方，向食指和中指方向延伸，末端有微微的上扬，且似乎有细小的分叉。</li><li>分析： 感情线长且上扬，通常代表重感情、对待爱情和家庭比较负责任。末端上扬指向食指和中指之间，说明您在处理感情时既有理想的追求，又能兼顾现实，性格温和，懂得体贴他人，但也可能偶尔会有情感细腻、容易多想的一面。</li></ul><p>4. 事业线（命运线，手掌中间垂直向上的线）</p><ul><li>观察： 照片中事业线相对浅淡，不像三大主线那么明显，可能只在中下部隐约可见。</li><li>分析： 事业线不明显并不代表没有事业，而是说明您可能不是一个事业心极强、非要追求大富大贵的人，或者您的职业发展比较自由、随性，不局限于传统的朝九晚五。您可能更偏向于顺其自然，靠自己的努力和多种技能谋生，或者事业重心在家庭和个人爱好上。</li></ul><p>5. 细纹与整体状态</p><ul><li>观察： 掌心中有很多细小的杂纹，交叉分布。手指指节明显。指肚看起来饱满。</li><li>分析： 满手细纹通常说明您心思细腻，感受力强，容易思虑过多，平时可能操心事比较多，容易因为琐事感到疲惫（建议注意压力管理和睡眠）。指节明显代表您性格踏实，做事注重细节和逻辑。手掌红润则表示当前气血运行较好，身体循环不错。</li></ul><p>💡 总结建议：</p><p>整体来看，这是一个偏向于理性、温和、重感情且心思细腻的手相。您可能在生活中是一个比较操心、追求完美的人。</p>','掌纹分析（手相学）属于传统的民俗文化，并没有科学依据，且手纹会随着时间、健康状况和生活环境的变化而改变。 以下分析仅供娱乐和参考，不能作为医学诊断或命运预测的依据。从照片来看，您的掌纹整体比较清晰，细纹较多，手掌颜色偏粉红，手指修长。以下是针对您主要掌纹的详细分析：1. 生命线（围绕大拇指根部的那条弧线）观察： 您的生命线清晰且弧度较大，向手腕方向延伸较好，没有明显的断裂。','',6,73,73,'published',NULL,NULL,NULL,0,1,0,1,2,0,0,0,0,'2026-08-28 11:27:35','','richtext',NULL,'','','2026-08-28 11:27:47','','2026-08-31 11:25:15','','73,69',0,NULL,0,0.00,'1'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('李白文章解析','article-1787893942705-666','<p><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">李白（701年—762年），字太白，号青莲居士，被后世尊称为“诗仙”。他不仅是唐代伟大的浪漫主义诗人，更是一位具有宏大政治抱负和深邃思想体系的思想家。他的文章与诗歌，既是个人情感的宣泄，也是盛唐时代精神的缩影。</span></p><h3><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">一、 思想内核：融汇百川的“思想家”</strong></h3><p><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">李白并非单纯的隐士或狂客，他构建了一个以“建功济世”为主导，以“安社稷、济苍生”为目标，以“功成身退”为个人归宿的思想体系。他的精神世界具有极强的包容性和矛盾性：</span></p><ol><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">爱国主义与英雄主义</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：他一生渴望“辅弼明主”，兼善天下，具有强烈的入世情怀和爱国热忱。</span></li><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">个性解放与自由平等</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：他蔑视权贵，追求平交王侯，向往风流飘逸、功成身退的道家境界。</span></li><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">现世与超脱的永恒拉扯</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：他极度渴望在现世实现抱负，却又深知现世的无常与不可把握。这种旺盛的生命力与现实的碰撞，造就了他内心深处“与尔同销万古愁”的永恒痛苦。</span></li></ol><h3><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">二、 人生阶段与文章风格演变</strong></h3><p><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">李白的创作生涯与其人生轨迹紧密相连，呈现出明显的阶段性特征：</span></p><ol><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">少年求学与出蜀壮游期（701-725年）</strong></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">核心情感</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：心怀天下，少年意气。</span></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">艺术特色</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：清丽空灵，善用白描与动态描写，充满蓬勃朝气与远大抱负。</span></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">代表作品</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：《渡荆门送别》（“山随平野尽，江入大荒流”），展现了辞亲远游的豪情与对故乡的眷恋。</span></li><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">长安供奉期（742-744年）</strong></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">核心情感</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：得意与孤傲并存，理想与现实的撕裂。</span></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">艺术特色</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：语言华美，意象两极分化（宫廷富贵与孤高意象交织）。</span></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">代表作品</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：《灞陵行送别》、《驾去温泉后赠杨山人》，表面歌颂宫廷，实则暗含对权贵倾轧的愤懑与短暂得意后的幻灭感。</span></li><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">南北漫游期（744-755年）</strong></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">核心情感</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：狂放与沉郁共生，个体愁思与宇宙宏大时空的对话。</span></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">艺术特色</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：雄奇想象，善用历史典故与对比，时空错位感强。</span></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">代表作品</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：《宣州谢朓楼饯别校书叔云》（“抽刀断水水更流，举杯消愁愁更愁”），在巨大的时空跨度中展现了政治理想破灭后的烦忧与对自由的终极渴望。</span></li><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">暮年忧国期（755-762年）</strong></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">核心情感</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：悲怆厚重，倔强不屈。</span></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">艺术特色</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：返璞归真，乐府诗现实化，语言凝练沉痛。</span></li><li data-list="bullet" class="ql-indent-1"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">代表作品</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：《豫章行》（“老母与子别，呼天野草间”），以质朴如史笔的语言，深刻描绘了安史之乱中的民生疾苦。</span></li></ol><h3><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">三、 经典文章与诗歌深度解析</strong></h3><p><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">李白留下了大量脍炙人口的诗文，其散文虽多为求人引荐的书信或宴饮序文，但同样字字珠玑。</span></p><ol><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">《春夜宴从弟桃李园序》</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：这篇散文是李白“不鸣则已，一鸣惊人”的典范。文章以“夫天地者，万物之逆旅也；光阴者，百代之过客也”开篇，将人生置于宏大的宇宙时空中，感叹“浮生若梦，为欢几何”。在阳春烟景中，他与兄弟们“开琼筵以坐花，飞羽觞而醉月”，展现了极高雅的生活情趣与旷达的生命哲学。</span></li><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">《将进酒》</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：这是一首豪放洒脱中的生命狂歌。开篇以“黄河之水天上来”的雄奇意象喻示时光不可逆，随即转入“天生我材必有用”的极度自信。全诗情感历经“悲—壮—狂—愁”的多重转折，表面推崇饮酒作乐，实则是对权贵的蔑视与怀才不遇的愤懑，最终以“与尔同销万古愁”将个人愁绪升华为千古文人的共同感慨。</span></li><li data-list="bullet"><span class="ql-ui" contenteditable="false"></span><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">《宣州谢朓楼饯别校书叔云》</strong><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">：这是一首以“愁”为内核、以“狂”为表象的巅峰之作。诗中“欲上青天揽明月”代表了盛唐气象下对高洁理想的无限向往，而“抽刀断水水更流”则猛然将人拉回残酷现实。这种大起大落的情感逻辑，展现了李白在矛盾中迸发的生命力度。</span></li></ol><h3><strong style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">四、 艺术成就与历史地位</strong></h3><p><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">李白的才华是超凡入圣的。他的五绝可与王维比肩，七绝不逊王昌龄，歌行体更是独占鳌头。他不仅诗才绝顶，还是书法名家，其真迹《上阳台帖》字画飘逸，豪气雄健。在词的创作上，传世的《菩萨蛮》和《忆秦娥》被誉为“百代词曲之祖”。</span></p><p><span style="background-color: rgb(255, 255, 255); color: rgb(13, 13, 13);">连“诗圣”杜甫都对他仰慕至极，认为“白也诗无敌，飘然思不群”，深知李白是仙才，自己的诗写得再好也只是人才。李白以其冲决一切的生命力、融汇百川的思想体系和卓绝的艺术才华，成为了中国文坛上光焰万丈的永恒偶像。</span></p><p><br></p>','李白（701年—762年），字太白，号青莲居士，被后世尊称为“诗仙”。他不仅是唐代伟大的浪漫主义诗人，更是一位具有宏大政治抱负和深邃思想体系的思想家。他的文章与诗歌，既是个人情感的宣泄，也是盛唐时代精神的缩影。 一、 思想内核：融汇百川的“思想家” 李白并非单纯的隐士或狂客，他构建了一个以“建功济世”为主导，以“安社稷',NULL,7,74,74,'published',1,'上帝发誓是 ','2026-08-28 17:55:00',0,0,0,0,4,0,0,0,0,'2026-08-28 17:55:00',NULL,'richtext','19db54dd-48ca-4685-8bfa-9430d44c0bcc',NULL,'','2026-08-28 13:12:22','','2026-08-28 17:55:00',NULL,'74,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('22222222','22222222','<h1 class="ql-align-center"><strong style="background-color: rgb(249, 250, 251); color: rgb(17, 24, 39);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="background-color: rgb(249, 250, 251); color: rgb(17, 24, 39);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="background-color: rgb(249, 250, 251); color: rgb(17, 24, 39);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><p><br></p>','22222222','http://127.0.0.1:9001/moyun/2026/08/28/64959c2a7418433fbf6a940ee2b2d4c7.png',6,77,77,'published',1,NULL,'2026-08-31 16:23:19',0,0,0,0,0,0,0,0,0,'2026-08-31 16:23:19',NULL,'richtext','874010b7-cce4-4ed1-a59c-26745b4e59c6',NULL,'','2026-08-28 14:01:48','','2026-08-31 16:23:19',NULL,'77,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('32242432','32242432','<ol><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span>斯巴达 啊v啊<strong><span class="ql-cursor">﻿﻿</span></strong></li></ol><p><br></p><iframe class="ql-video" frameborder="0" allowfullscreen="true" src="伤风败俗"></iframe><blockquote><br></blockquote><p><img src="http://127.0.0.1:9001/moyun/2026/08/28/5b6a82da1d7b443cb6e36cc66fc90b36.png"> </p>','本文围绕标题“32242432”与正文“斯巴达”等有限信息，生成内容摘要与SEO优化信息，适合测试或占位页面使用。',NULL,6,74,74,'published',1,'撒旦v撒','2026-08-28 17:58:00',0,0,0,0,4,0,0,0,0,'2026-08-28 17:58:00',NULL,'richtext','acee5fca-2b00-4806-b11f-ba163fc924a9',NULL,'','2026-08-28 17:50:27','','2026-09-02 11:05:00',NULL,'74,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('处暑过后，傍晚的风开始凉了','article-1788746586411-390','<p><span style="background-color: rgb(255, 255, 255); color: rgb(17, 24, 39);">写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。</span></p><p><span style="background-color: rgb(255, 255, 255); color: rgb(17, 24, 39);"> 写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。</span></p><p><span style="background-color: rgb(255, 255, 255); color: rgb(17, 24, 39);"> 写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。 </span></p><p><span style="background-color: rgb(255, 255, 255); color: rgb(17, 24, 39);">写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。</span></p><p><br></p>','写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。','http://127.0.0.1:9001/moyun/2026/09/07/37619d86ff7b474e9aacd4f737c84d9c.jpeg',6,73,73,'published',1,NULL,'2026-09-07 10:05:37',0,0,0,0,1,0,0,0,0,'2026-09-07 10:05:37',NULL,'richtext','c91db263-27da-404a-9575-4a20523cced2',NULL,'','2026-09-07 10:03:06','','2026-09-07 10:06:00',NULL,'73,69',0,NULL,0,0.00,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                                                      ('手机端发起的第一个文章','article-1788747328228-239','<p>http://localhost:3000/前台首页需要做一次ui布局样式等重构，请根据原型html 格局语义修改布局，其中包括头部，中部，尾部，因为重构的原型可能缺少一些元素，在改造过程中，缺失的内容需要补上，放在合理位置，尤其是头部的 帮助中心，主题切换，搜索，消息等小的按钮，原型在moyun-project-document\\docs\\08-原型设计目录下，其中有两个文件，一个V1是用户未登录时展示的页面：旭林知行_V1.html，另一个V2是用户登录后展示的样子：旭林知行_V2.html，请根据实际优化</p>','<p>http://localhost:3000/前台首页需要做一次ui布局样式等重构，请根据原型html 格局语义修改布局，其中包括头部，中部，尾部，因为重构的原型可能缺少一些元素，在改造过程中，缺失的内容需要补上，放在合理位置，尤其是头部的 帮助中心，主题切换，搜索，消息等小的按钮，原型在moyun-project-document\\docs\\08-原型设计目录下，其中有两个文件，一个V1是用...',NULL,6,76,76,'published',1,NULL,'2026-09-07 10:15:49',0,0,0,0,0,0,0,0,0,'2026-09-07 10:15:49',NULL,'richtext','9513579a-a4d2-4293-a8d1-9ab845f680c2',NULL,'','2026-09-07 10:15:28','','2026-09-07 10:15:49',NULL,'76,69',0,NULL,0,0.00,'0');
INSERT INTO `moyun-db`.portal_article_version (article_id,version_no,title,content,content_markdown,excerpt,operator_id,created_time) VALUES
    (14,1,'22222222','<h1 class="ql-align-center"><strong style="background-color: rgb(249, 250, 251); color: rgb(17, 24, 39);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="background-color: rgb(249, 250, 251); color: rgb(17, 24, 39);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="background-color: rgb(249, 250, 251); color: rgb(17, 24, 39);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><h1 class="ql-align-center"><strong style="color: rgb(17, 24, 39); background-color: rgb(249, 250, 251);">李白文章解析</strong></h1><p><br></p>',NULL,'22222222',6,'2026-08-28 14:01:48');
INSERT INTO `moyun-db`.portal_article_view (article_id,user_id,ip,view_time,user_agent,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                               (1,6,'0:0:0:0:0:0:0:1','2026-08-28 09:58:42','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 09:58:42','','2026-08-28 09:58:42',NULL),
                                                                                                                                               (1,7,'0:0:0:0:0:0:0:1','2026-08-28 10:02:40','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 10:02:39','','2026-08-28 10:02:39',NULL),
                                                                                                                                               (1,7,'0:0:0:0:0:0:0:1','2026-08-28 10:20:56','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 10:20:55','','2026-08-28 10:20:55',NULL),
                                                                                                                                               (1,7,'0:0:0:0:0:0:0:1','2026-08-28 10:27:26','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 10:27:25','','2026-08-28 10:27:25',NULL),
                                                                                                                                               (12,7,'0:0:0:0:0:0:0:1','2026-08-28 13:28:11','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:28:10','','2026-08-28 13:28:10',NULL),
                                                                                                                                               (1,7,'0:0:0:0:0:0:0:1','2026-08-28 13:29:29','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:29:29','','2026-08-28 13:29:29',NULL),
                                                                                                                                               (9,7,'0:0:0:0:0:0:0:1','2026-08-28 13:31:03','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:31:03','','2026-08-28 13:31:03',NULL),
                                                                                                                                               (13,7,'0:0:0:0:0:0:0:1','2026-08-28 13:35:55','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:35:54','','2026-08-28 13:35:54',NULL),
                                                                                                                                               (1,7,'0:0:0:0:0:0:0:1','2026-08-28 13:36:19','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:36:19','','2026-08-28 13:36:19',NULL),
                                                                                                                                               (10,7,'0:0:0:0:0:0:0:1','2026-08-28 13:36:33','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:36:33','','2026-08-28 13:36:33',NULL);
INSERT INTO `moyun-db`.portal_article_view (article_id,user_id,ip,view_time,user_agent,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                               (6,7,'0:0:0:0:0:0:0:1','2026-08-28 13:37:35','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:37:34','','2026-08-28 13:37:34',NULL),
                                                                                                                                               (8,7,'0:0:0:0:0:0:0:1','2026-08-28 13:37:37','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:37:36','','2026-08-28 13:37:36',NULL),
                                                                                                                                               (5,7,'0:0:0:0:0:0:0:1','2026-08-28 13:37:39','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:37:39','','2026-08-28 13:37:39',NULL),
                                                                                                                                               (13,7,'0:0:0:0:0:0:0:1','2026-08-28 13:47:01','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:47:01','','2026-08-28 13:47:01',NULL),
                                                                                                                                               (1,7,'0:0:0:0:0:0:0:1','2026-08-28 13:47:19','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:47:19','','2026-08-28 13:47:19',NULL),
                                                                                                                                               (13,6,'0:0:0:0:0:0:0:1','2026-08-28 13:49:24','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:49:23','','2026-08-28 13:49:23',NULL),
                                                                                                                                               (1,6,'0:0:0:0:0:0:0:1','2026-08-28 13:49:34','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:49:33','','2026-08-28 13:49:33',NULL),
                                                                                                                                               (12,6,'0:0:0:0:0:0:0:1','2026-08-28 13:49:36','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:49:35','','2026-08-28 13:49:35',NULL),
                                                                                                                                               (2,6,'0:0:0:0:0:0:0:1','2026-08-28 13:53:43','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:53:42','','2026-08-28 13:53:42',NULL),
                                                                                                                                               (9,6,'0:0:0:0:0:0:0:1','2026-08-28 13:59:45','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 13:59:45','','2026-08-28 13:59:45',NULL);
INSERT INTO `moyun-db`.portal_article_view (article_id,user_id,ip,view_time,user_agent,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                               (2,NULL,'0:0:0:0:0:0:0:1','2026-08-28 14:00:15','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 14:00:14','','2026-08-28 14:00:14',NULL),
                                                                                                                                               (9,NULL,'0:0:0:0:0:0:0:1','2026-08-28 15:35:10','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36 Edg/151.0.0.0','','2026-08-28 15:35:10','','2026-08-28 15:35:10',NULL),
                                                                                                                                               (13,6,'0:0:0:0:0:0:0:1','2026-08-28 16:01:41','Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Mobile Safari/537.36','','2026-08-28 16:01:40','','2026-08-28 16:01:40',NULL),
                                                                                                                                               (9,6,'0:0:0:0:0:0:0:1','2026-08-28 16:01:46','Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Mobile Safari/537.36','','2026-08-28 16:01:45','','2026-08-28 16:01:45',NULL),
                                                                                                                                               (9,6,'0:0:0:0:0:0:0:1','2026-08-28 16:17:21','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 16:17:21','','2026-08-28 16:17:21',NULL),
                                                                                                                                               (10,6,'0:0:0:0:0:0:0:1','2026-08-28 16:17:27','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 16:17:26','','2026-08-28 16:17:26',NULL),
                                                                                                                                               (8,6,'0:0:0:0:0:0:0:1','2026-08-28 16:17:30','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 16:17:30','','2026-08-28 16:17:30',NULL),
                                                                                                                                               (9,NULL,'0:0:0:0:0:0:0:1','2026-08-28 16:45:33','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36 Edg/151.0.0.0','','2026-08-28 16:45:32','','2026-08-28 16:45:32',NULL),
                                                                                                                                               (16,6,'0:0:0:0:0:0:0:1','2026-08-28 17:59:32','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-28 17:59:32','','2026-08-28 17:59:32',NULL),
                                                                                                                                               (16,NULL,'0:0:0:0:0:0:0:1','2026-08-29 03:20:43','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-29 03:20:42','','2026-08-29 03:20:42',NULL);
INSERT INTO `moyun-db`.portal_article_view (article_id,user_id,ip,view_time,user_agent,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                               (16,NULL,'0:0:0:0:0:0:0:1','2026-08-29 07:43:31','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-29 07:43:30','','2026-08-29 07:43:30',NULL),
                                                                                                                                               (3,NULL,'0:0:0:0:0:0:0:1','2026-08-31 13:08:19','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-31 13:08:19','','2026-08-31 13:08:19',NULL),
                                                                                                                                               (1,NULL,'0:0:0:0:0:0:0:1','2026-08-31 13:09:05','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-31 13:09:05','','2026-08-31 13:09:05',NULL),
                                                                                                                                               (7,NULL,'0:0:0:0:0:0:0:1','2026-08-31 13:09:42','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-31 13:09:41','','2026-08-31 13:09:41',NULL),
                                                                                                                                               (2,6,'0:0:0:0:0:0:0:1','2026-08-31 16:20:39','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-08-31 16:20:39','','2026-08-31 16:20:39',NULL),
                                                                                                                                               (2,6,'0:0:0:0:0:0:0:1','2026-09-02 11:01:08','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 11:01:08','','2026-09-02 11:01:08',NULL),
                                                                                                                                               (4,6,'0:0:0:0:0:0:0:1','2026-09-02 11:01:17','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 11:01:17','','2026-09-02 11:01:17',NULL),
                                                                                                                                               (7,6,'0:0:0:0:0:0:0:1','2026-09-02 11:03:16','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 11:03:16','','2026-09-02 11:03:16',NULL),
                                                                                                                                               (16,6,'0:0:0:0:0:0:0:1','2026-09-02 11:05:00','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 11:05:00','','2026-09-02 11:05:00',NULL),
                                                                                                                                               (2,6,'0:0:0:0:0:0:0:1','2026-09-02 11:24:23','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 11:24:23','','2026-09-02 11:24:23',NULL);
INSERT INTO `moyun-db`.portal_article_view (article_id,user_id,ip,view_time,user_agent,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                               (7,6,'0:0:0:0:0:0:0:1','2026-09-02 11:28:58','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 11:28:57','','2026-09-02 11:28:57',NULL),
                                                                                                                                               (9,NULL,'0:0:0:0:0:0:0:1','2026-09-02 13:11:42','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 13:11:42','','2026-09-02 13:11:42',NULL),
                                                                                                                                               (11,6,'0:0:0:0:0:0:0:1','2026-09-02 14:22:17','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 14:22:16','','2026-09-02 14:22:16',NULL),
                                                                                                                                               (5,6,'0:0:0:0:0:0:0:1','2026-09-02 14:22:36','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36','','2026-09-02 14:22:36','','2026-09-02 14:22:36',NULL),
                                                                                                                                               (2,6,'0:0:0:0:0:0:0:1','2026-09-03 15:08:11','Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Mobile Safari/537.36','','2026-09-03 15:08:10','','2026-09-03 15:08:10',NULL),
                                                                                                                                               (9,NULL,'0:0:0:0:0:0:0:1','2026-09-03 16:12:04','Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Mobile Safari/537.36','','2026-09-03 16:12:03','','2026-09-03 16:12:03',NULL),
                                                                                                                                               (7,NULL,'0:0:0:0:0:0:0:1','2026-09-07 09:21:42','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36','','2026-09-07 09:21:41','','2026-09-07 09:21:41',NULL),
                                                                                                                                               (17,6,'0:0:0:0:0:0:0:1','2026-09-07 10:06:01','Mozilla/5.0 (Linux; Android 16; V2506A Build/BP2A.250605.031.A3_V000L1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/97.0.4692.98 Mobile Safari/537.36 T7/13.40 SP-engine/3.62.0 bd_dvt/0 baiduboxapp/15.72.5.10 (Baidu; P1 16) NABar/1.0','','2026-09-07 10:06:00','','2026-09-07 10:06:00',NULL),
                                                                                                                                               (9,NULL,'0:0:0:0:0:0:0:1','2026-09-11 13:38:17','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36','','2026-09-11 13:38:16','','2026-09-11 13:38:16',NULL),
                                                                                                                                               (8,NULL,'0:0:0:0:0:0:0:1','2026-09-11 13:38:25','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36','','2026-09-11 13:38:25','','2026-09-11 13:38:25',NULL);
INSERT INTO `moyun-db`.portal_article_view (article_id,user_id,ip,view_time,user_agent,create_by,create_time,update_by,update_time,remark) VALUES
    (9,NULL,'0:0:0:0:0:0:0:1','2026-09-11 16:12:59','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36','','2026-09-11 16:12:58','','2026-09-11 16:12:58',NULL);
INSERT INTO `moyun-db`.portal_bookmark (user_id,article_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                   (7,9,'','2026-08-28 13:32:13','','2026-08-28 13:32:12',NULL),
                                                                                                                   (6,2,'','2026-08-28 13:53:47','','2026-08-28 13:53:46',NULL),
                                                                                                                   (6,5,'','2026-09-02 14:22:40','','2026-09-02 14:22:39',NULL);
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                          ('首页','home','精选推荐、双轨轮播','fa-home',1,0,'0',1,'home','/',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('面试专区','interview','AI 语音面试、面经复盘、简历优化','fa-briefcase',2,0,'0',1,'static','/interview',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('学习中心','learn','题库、刷题、错题本、学习计划','fa-graduation-cap',3,0,'0',1,'static','/learn',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('阅读空间','reading','散文天地、技术笔记、读书空间','fa-book',4,0,'0',1,'static','/reading',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('创作互动','creation','话题、动态、专栏、征文、发布','fa-feather',5,0,'0',1,'static','/creation',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('我的','mine','个人中心、成长时间线、我的内容','fa-user',6,0,'0',1,'static','/user',NULL,'directory',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('AI 语音面试官','interview-voice','AI 语音面试官，真实面试场景模拟','fa-microphone',1,52,'0',1,'static','/interview/voice','NEW','special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('面试经验','interview-experiences','大厂面试全流程还原','fa-chart-line',2,52,'0',1,'static','/interview/experiences',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('简历模板','interview-resume-templates','技术亮点提炼、项目描述技巧','fa-file-alt',3,52,'0',1,'static','/interview/resume-templates',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('面试题库','learn-questions','算法题、系统设计、行为面试','fa-clipboard-list',1,53,'0',1,'static','/learn/questions',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                          ('刷题中心','learn-practice','在线编程、选择题练习','fa-laptop-code',2,53,'0',1,'static','/learn/practice','HOT','directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('错题本','learn-wrong','错题归集与复习','fa-times-circle',3,53,'0',1,'static','/learn/wrong',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('知识图谱','learn-knowledge','知识体系可视化','fa-project-diagram',4,53,'0',1,'static','/learn/knowledge',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('刷题排行榜','learn-leaderboard','刷题榜、学习榜','fa-trophy',5,53,'0',1,'static','/learn/leaderboard',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('刷题日历','learn-calendar','刷题打卡日历','fa-calendar-check',6,53,'0',1,'static','/learn/calendar',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('学习计划','learn-plan','个人学习计划管理','fa-calendar-alt',7,53,'0',1,'static','/learn/plan',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('选择题','learn-practice-choice','选择题在线练习','fa-check-square',1,61,'0',1,'static','/learn/practice/choice',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('编程题','learn-practice-coding','编程题在线练习','fa-code',2,61,'0',1,'static','/learn/practice/coding',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('散文天地','prose','人文书写与情感表达','fa-pen-fancy',1,54,'0',1,'category','/category/prose',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('技术笔记','tech-notes','开发记录、技术解析、AI编程实践','fa-code',2,54,'0',1,'category','/category/tech-notes',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                          ('读书空间','reading-space','发现好书、我的书架、金句摘录','fa-book-reader',3,54,'0',1,'static','/reading/space',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('金句摘录','reading-quotes','跨分区高光语句精选','fa-quote-left',4,54,'0',1,'static','/reading/quotes',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-09-02 13:52:37',NULL,'1'),
                                                                                                                                                                                                                                          ('人间烟火','life-stories','饮食、市井、生活琐记','fa-utensils',1,69,'0',1,'category','/category/life-stories',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('山河行吟','travel-nature','游记、自然书写、生态散文','fa-mountain',2,69,'0',1,'category','/category/travel-nature',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('心灵独白','inner-thoughts','孤独、成长、疗愈随笔','fa-heart',3,69,'0',1,'category','/category/inner-thoughts',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('城市笔记','city-notes','北上广深、小镇观察','fa-city',4,69,'0',1,'category','/category/city-notes',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('四季专栏','seasons','春之思、夏之躁、秋之静、冬之藏','fa-leaf',5,69,'0',1,'category','/category/seasons',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('声音散文','audio-prose','作者自读、背景音效沉浸体验','fa-volume-up',6,69,'0',1,'category','/category/audio-prose',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('读者来信','reader-letters','短篇心声刊发与回声计划','fa-envelope',7,69,'0',1,'category','/category/reader-letters',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('技术栈手册','tech-stack','Java/SpringBoot、React/Vue、Flutter/UniApp','fa-book-open',1,70,'0',1,'category','/category/tech-stack',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                          ('架构札记','architecture','微服务、缓存策略、分布式事务','fa-project-diagram',2,70,'0',1,'category','/category/architecture',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('性能日志','performance','SQL优化、前端加载、JVM调优','fa-tachometer-alt',3,70,'0',1,'category','/category/performance',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('AI编程','ai-coding','Cursor使用、ChatGPT提示工程、AI排错记录','fa-robot',4,70,'0',1,'category','/category/ai-coding',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('开源日志','open-source','PR提交、Issue解决、源码阅读','fa-code-branch',5,70,'0',1,'category','/category/open-source',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('新手入门','beginner','环境配置、第一行代码实录','fa-play-circle',6,70,'0',1,'category','/category/beginner',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('发现好书','reading-discover','发现好书、书单推荐','fa-list',1,71,'0',1,'static','/reading/discover',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('我的书架','reading-bookshelf','个人书架管理','fa-bookmark',2,71,'0',1,'static','/reading/bookshelf',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('金句摘录','reading-space-quotes','读书空间内的金句摘录','fa-quote-left',3,71,'0',1,'static','/reading/quotes',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('话题广场','topics','话题讨论列表','fa-comments',1,55,'0',1,'static','/topics',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('动态广场','feed','用户动态流','fa-stream',2,55,'0',1,'static','/feed',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                          ('专栏广场','columns','专栏列表与订阅','fa-columns',3,55,'0',1,'static','/columns',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('征文活动','contests','征文活动、技术挑战赛','fa-file-upload',4,55,'0',1,'static','/contests',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('发布文章','publish','发布新文章（快捷入口）','fa-edit',5,55,'0',1,'static','/publish',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('成长排行榜','ranking','成长值排行榜','fa-trophy',6,55,'0',1,'static','/ranking',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('个人中心','user','个人中心主页','fa-user-circle',1,56,'0',1,'static','/user',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('成长时间线','growth-timeline','成长记录时间线','fa-chart-line',2,56,'0',1,'static','/growth/timeline',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('我的文章','my-articles','我发布的文章','fa-file-alt',3,56,'0',1,'static','/my/articles',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('我的专栏','column-my','我创建的专栏','fa-columns',4,56,'0',1,'static','/column/my',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('我的成就','achievements','我的成就与徽章','fa-award',5,56,'0',1,'static','/achievements',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
                                                                                                                                                                                                                                          ('我的话题观点','topic-my','我发起的话题与观点','fa-comments',6,56,'0',1,'static','/topic/my',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
    ('面试指南','interview','面试中心主入口',NULL,0,52,'0',1,'static','/interview',NULL,'special',0,'','2026-09-02 13:23:29','','2026-09-02 13:23:29',NULL,'0');
INSERT INTO `moyun-db`.portal_column (user_id,title,subtitle,description,cover,category_id,status,auditor_id,audit_remark,audit_time,article_count,subscribe_count,view_count,is_finished,price,created_time,updated_time,del_flag) VALUES
    (6,'1222222222','12222','122222222',NULL,NULL,'published',NULL,NULL,NULL,3,1,2,0,0.00,'2026-08-28 16:24:35','2026-08-31 16:19:08','0');
INSERT INTO `moyun-db`.portal_column_article (column_id,article_id,sort_order,created_time) VALUES
                                                                                                (1,12,0,'2026-08-28 16:24:36'),
                                                                                                (1,7,2,'2026-08-31 15:16:20'),
                                                                                                (1,8,4,'2026-08-31 15:16:20');
INSERT INTO `moyun-db`.portal_column_subscribe (column_id,user_id,created_time) VALUES
    (1,6,'2026-08-28 16:24:50');
INSERT INTO `moyun-db`.portal_comment (article_id,author_id,content,parent_id,root_id,reply_to,reply_to_content,like_count,status,auditor_id,audit_remark,audit_time,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                      (9,6,'定时发布是说的',0,0,NULL,'',1,'1',NULL,NULL,NULL,'','2026-08-28 16:02:09','','2026-08-28 16:02:14',NULL,'0'),
                                                                                                                                                                                                                                      (9,6,'算法识别啊',1,1,6,'定时发布是说的',0,'1',NULL,NULL,NULL,'','2026-08-28 16:02:15','','2026-08-28 16:02:15',NULL,'0'),
                                                                                                                                                                                                                                      (9,6,'打算把啊 ',1,1,6,'定时发布是说的',0,'1',NULL,NULL,NULL,'','2026-08-28 16:02:24','','2026-08-28 16:02:24',NULL,'0');
INSERT INTO `moyun-db`.portal_comment_like (user_id,comment_id,create_by,create_time,update_by,update_time,remark) VALUES
    (6,1,'','2026-08-28 16:02:14','','2026-08-28 16:02:14',NULL);
INSERT INTO `moyun-db`.portal_creator_certification (user_id,real_name,cert_type,cert_no,cert_no_enc,cert_no_mask,derived_gender,derived_birth,verify_channel,verify_serial,cert_image,cert_image_front,cert_image_back,intro,works,status,auditor_id,audit_remark,created_time,audited_time,del_flag) VALUES
    (7,'李白','identity',NULL,'enc:v1:Ak4GFR3duJ/2YvaQ:akQkdvJgBJ4oeSK4EVSLCNdYt8BYtm3cV7CiQ/8Ggye5Pg==','532***********0730','男','1996-06-19','manual',NULL,'http://127.0.0.1:9001/moyun/2026/08/28/7eaf5ae9c5034a819ba7e86dd32d4061.jpg','http://127.0.0.1:9001/moyun/2026/08/28/7eaf5ae9c5034a819ba7e86dd32d4061.jpg','http://127.0.0.1:9001/moyun/2026/08/28/10ada7c421034447a681e632f2f164a1.png','','','approved',1,'','2026-08-28 10:03:46','2026-08-28 10:04:19','0');
INSERT INTO `moyun-db`.portal_entity_tag (tag_id,entity_type,entity_id,sort,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                    (29,'article',2,0,'','2026-08-28 11:02:01','','2026-08-28 11:02:01',NULL),
                                                                                                                                    (31,'article',2,1,'','2026-08-28 11:02:01','','2026-08-28 11:02:01',NULL),
                                                                                                                                    (35,'article',4,0,'','2026-08-28 11:13:22','','2026-08-28 11:13:21',NULL),
                                                                                                                                    (36,'article',4,1,'','2026-08-28 11:13:22','','2026-08-28 11:13:21',NULL),
                                                                                                                                    (52,'article',4,2,'','2026-08-28 11:13:22','','2026-08-28 11:13:21',NULL),
                                                                                                                                    (31,'article',4,3,'','2026-08-28 11:13:22','','2026-08-28 11:13:21',NULL),
                                                                                                                                    (32,'article',5,0,'','2026-08-28 11:14:42','','2026-08-28 11:14:41',NULL),
                                                                                                                                    (29,'article',5,1,'','2026-08-28 11:14:42','','2026-08-28 11:14:41',NULL),
                                                                                                                                    (30,'article',5,2,'','2026-08-28 11:14:42','','2026-08-28 11:14:41',NULL),
                                                                                                                                    (30,'article',6,0,'','2026-08-28 11:15:30','','2026-08-28 11:15:30',NULL);
INSERT INTO `moyun-db`.portal_entity_tag (tag_id,entity_type,entity_id,sort,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                    (31,'article',6,1,'','2026-08-28 11:15:30','','2026-08-28 11:15:30',NULL),
                                                                                                                                    (32,'article',8,0,'','2026-08-28 11:22:26','','2026-08-28 11:22:25',NULL),
                                                                                                                                    (34,'article',8,1,'','2026-08-28 11:22:26','','2026-08-28 11:22:25',NULL),
                                                                                                                                    (35,'article',8,2,'','2026-08-28 11:22:26','','2026-08-28 11:22:25',NULL),
                                                                                                                                    (30,'article',8,3,'','2026-08-28 11:22:26','','2026-08-28 11:22:25',NULL),
                                                                                                                                    (33,'article',9,0,'','2026-08-28 11:23:44','','2026-08-28 11:23:44',NULL),
                                                                                                                                    (34,'article',9,1,'','2026-08-28 11:23:44','','2026-08-28 11:23:44',NULL),
                                                                                                                                    (35,'article',9,2,'','2026-08-28 11:23:44','','2026-08-28 11:23:44',NULL),
                                                                                                                                    (33,'article',11,0,'','2026-08-28 11:26:14','','2026-08-28 11:26:13',NULL),
                                                                                                                                    (35,'article',11,1,'','2026-08-28 11:26:14','','2026-08-28 11:26:13',NULL);
INSERT INTO `moyun-db`.portal_entity_tag (tag_id,entity_type,entity_id,sort,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                    (57,'interview_experience',1,0,'','2026-08-31 10:42:07','','2026-08-31 10:42:06',NULL),
                                                                                                                                    (35,'article',17,0,'','2026-09-07 10:03:07','','2026-09-07 10:03:06',NULL),
                                                                                                                                    (39,'article',17,1,'','2026-09-07 10:03:07','','2026-09-07 10:03:06',NULL),
                                                                                                                                    (40,'article',17,2,'','2026-09-07 10:03:07','','2026-09-07 10:03:06',NULL),
                                                                                                                                    (57,'article',17,3,'','2026-09-07 10:03:07','','2026-09-07 10:03:06',NULL),
                                                                                                                                    (34,'article',18,0,'','2026-09-07 10:15:28','','2026-09-07 10:15:28',NULL),
                                                                                                                                    (36,'article',18,1,'','2026-09-07 10:15:28','','2026-09-07 10:15:28',NULL),
                                                                                                                                    (29,'article',18,2,'','2026-09-07 10:15:28','','2026-09-07 10:15:28',NULL),
                                                                                                                                    (31,'article',18,3,'','2026-09-07 10:15:28','','2026-09-07 10:15:28',NULL);
INSERT INTO `moyun-db`.portal_feed_event (user_id,event_type,target_type,target_id,title,summary,cover,created_time) VALUES
                                                                                                                         (6,'publish_article','article',1,'22222222222222222222222222','222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222',NULL,'2026-08-28 09:58:11'),
                                                                                                                         (7,'publish_article','article',13,'李白文章解析','李白（701年—762年），字太白，号青莲居士，被后世尊称为“诗仙”。他不仅是唐代伟大的浪漫主义诗人，更是一位具有宏大政治抱负和深邃思想体系的思想家。他的文章与诗歌，既是个人情感的宣泄，也是盛唐时代精神的缩影。 一、 思想内核：融汇百川的“思想家” 李白并非单纯的隐士或狂客，他构建了一个以“建功济世”为主导，以“安社稷',NULL,'2026-08-28 13:12:23'),
                                                                                                                         (7,'solve_question','question',1,'TCP三次握手的目的是什么？','easy',NULL,'2026-08-28 13:44:02'),
                                                                                                                         (7,'solve_question','question',5,'Redis中以下哪种数据类型不支持？','medium',NULL,'2026-08-28 13:44:45'),
                                                                                                                         (6,'publish_article','article',16,'32242432','本文围绕标题“32242432”与正文“斯巴达”等有限信息，生成内容摘要与SEO优化信息，适合测试或占位页面使用。',NULL,'2026-08-28 17:50:28'),
                                                                                                                         (7,'publish_article','article',7,'web 端如何实现一个语音转文字的','Web 端实现语音转文字，主要有以下几种方案，从简单到复杂依次介绍：方案一：Web Speech API（最简单，推荐先用这个）这是浏览器原生提供的能力，零依赖、无需后端，几行代码就能跑起来。完整示例代码html预览<!','','2026-08-28 17:54:45'),
                                                                                                                         (6,'publish_experience','experience',1,'java面试','面试',NULL,'2026-08-31 10:42:07'),
                                                                                                                         (6,'publish_article','article',14,'22222222','22222222','http://127.0.0.1:9001/moyun/2026/08/28/64959c2a7418433fbf6a940ee2b2d4c7.png','2026-08-31 16:23:19'),
                                                                                                                         (6,'publish_article','article',17,'处暑过后，傍晚的风开始凉了','写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。','http://127.0.0.1:9001/moyun/2026/09/07/37619d86ff7b474e9aacd4f737c84d9c.jpeg','2026-09-07 10:03:07'),
                                                                                                                         (6,'publish_article','article',18,'手机端发起的第一个文章','<p>http://localhost:3000/前台首页需要做一次ui布局样式等重构，请根据原型html 格局语义修改布局，其中包括头部，中部，尾部，因为重构的原型可能缺少一些元素，在改造过程中，缺失的内容需要补上，放在合理位置，尤其是头部的 帮助中心，主题切换，搜索，消息等小的按钮，原型在moyun-project-document\\docs\\08-原型设计目录下，其中有两个文件，一个V1是用...',NULL,'2026-09-07 10:15:28');
INSERT INTO `moyun-db`.portal_feed_event (user_id,event_type,target_type,target_id,title,summary,cover,created_time) VALUES
                                                                                                                         (6,'solve_question','question',1,'TCP三次握手的目的是什么？','easy',NULL,'2026-09-07 17:08:33'),
                                                                                                                         (6,'create_topic','topic',1,'哈哈哈，世界太变态了？','世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？','http://127.0.0.1:9001/moyun/2026/09/07/59caaf2c79f94d9c804ddb6618cbfe7e.jpg','2026-09-07 17:52:45');
INSERT INTO `moyun-db`.portal_feed_inbox (user_id,event_id,created_time) VALUES
    (6,6,'2026-08-28 17:54:45');
INSERT INTO `moyun-db`.portal_feedback (feedback_type,subject,description,contact,user_id,username,ip,status,handler,handle_result,handle_time,create_time,update_time,remark,del_flag) VALUES
                                                                                                                                                                                            ('suggestion','连注册都没有','没有注册功能',NULL,6,'zhangsan','0:0:0:0:0:0:0:1','resolved','admin','好的，我处理','2026-09-08 14:34:40','2026-09-08 14:23:49','2026-09-08 14:34:40',NULL,'0'),
                                                                                                                                                                                            ('suggestion','解决一个问题吧','大v阿萨俺的大俺的大啊',NULL,6,'zhangsan','0:0:0:0:0:0:0:1','pending',NULL,NULL,NULL,'2026-09-08 17:36:32','2026-09-08 17:36:32',NULL,'0');
INSERT INTO `moyun-db`.portal_follow (follower_id,following_id,create_by,create_time,update_by,update_time,remark) VALUES
    (6,7,'','2026-09-02 11:24:34','','2026-09-02 11:24:33',NULL);
INSERT INTO `moyun-db`.portal_friend_link (name,url,description,logo,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                  ('中国作家网','https://www.chinawriter.com.cn','中国作家协会官方网站',NULL,1,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0'),
                                                                                                                                                  ('起点中文网','https://www.qidian.com','阅文集团旗下网站',NULL,2,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0'),
                                                                                                                                                  ('掘金','https://juejin.cn','帮助开发者成长的社区',NULL,3,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0');
INSERT INTO `moyun-db`.portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                    ('article','publish_article',50,3,'发布文章','0',1,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('article','receive_like',2,0,'文章被点赞','0',2,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('article','receive_bookmark',3,0,'文章被收藏','0',3,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('user','receive_follow',5,0,'被关注','0',4,'','2026-07-28 15:48:22','','2026-08-28 01:23:04',NULL),
                                                                                                                                                                    ('article','article_featured',100,0,'文章被精选','0',5,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('article','receive_comment',2,0,'文章被评论','0',6,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('reading','finish_book',20,1,'完成阅读一本书','0',10,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('reading','write_quote',15,0,'发布金句','0',11,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('reading','create_booklist',20,0,'创建书单','0',12,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('reading','quote_liked',5,0,'金句被点赞','0',13,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL);
INSERT INTO `moyun-db`.portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                    ('reading','booklist_liked',5,0,'书单被点赞','0',14,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('reading','booklist_bookmarked',10,0,'书单被收藏','0',15,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('interview','solve_question',10,20,'解题','0',20,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('interview','write_note',15,0,'写笔记','0',21,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('interview','note_adopted',50,0,'笔记被精选','0',22,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('interview','publish_experience',30,0,'发布面经','0',23,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('interview','experience_liked',2,0,'面经被点赞','0',24,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('interview','experience_bookmarked',3,0,'面经被收藏','0',25,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('all','daily_checkin',1,1,'每日签到','0',30,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
                                                                                                                                                                    ('all','daily_login',1,1,'每日登录','0',31,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL);
INSERT INTO `moyun-db`.portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                    ('article','receive_tip',3,0,'文章/专栏被打赏','0',7,'','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),
                                                                                                                                                                    ('article','tip_others',1,3,'打赏他人','0',8,'','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),
                                                                                                                                                                    ('topic','create_topic',10,0,'发起话题','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
                                                                                                                                                                    ('topic','post_opinion',2,10,'发表观点','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
                                                                                                                                                                    ('topic','receive_topic_like',2,0,'话题被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
                                                                                                                                                                    ('topic','receive_post_like',2,0,'观点被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
                                                                                                                                                                    ('topic','receive_topic_comment',2,0,'话题被评论','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
                                                                                                                                                                    ('topic','receive_post_comment',2,0,'观点被评论','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
                                                                                                                                                                    ('topic','receive_comment_like',2,0,'评论被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
                                                                                                                                                                    ('topic','topic_featured',50,0,'话题被精选','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL);
INSERT INTO `moyun-db`.portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
    ('interview','read_question',1,20,'阅读题目','0',19,'admin','2026-08-31 09:11:49','',NULL,'题库阅读学习行为，同题每日仅记一次');
INSERT INTO `moyun-db`.portal_help_category (name,icon,description,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                ('发布与编辑','BookOpen','文章发布、编辑、删除等操作指南',1,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
                                                                                                                                                ('账号与安全','HelpCircle','登录、注册、密码、安全设置',2,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
                                                                                                                                                ('互动功能','MessageSquare','评论、点赞、关注等互动功能',3,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
                                                                                                                                                ('社区规则','Shield','使用规范、违规处理、隐私政策',4,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0');
INSERT INTO `moyun-db`.portal_import_template_config (business_key,field_name,column_name,description,example_value,required,field_type,dict_type,combo_values,column_width,sort,status,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                                                                                                ('interview_question','title','题目标题','必填，不超过500字','TCP三次握手的目的是什么？',1,'string',NULL,NULL,30,1,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','description','题目描述','可选，题目详细描述','TCP协议建立连接时使用三次握手，请选择其主要目的。',0,'string',NULL,NULL,40,2,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','difficulty','难度','可选，下拉选择','easy',0,'dict',NULL,'easy,medium,hard',12,3,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','categoryId','分类ID','可选，数字类型，对应面试分类表ID','1',0,'number',NULL,NULL,12,4,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','tags','标签','可选，逗号分隔','TCP,网络,三次握手',0,'string',NULL,NULL,20,5,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','companies','公司','可选，逗号分隔','字节跳动,腾讯,阿里',0,'string',NULL,NULL,20,6,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','hint','提示','可选，做题时的提示信息','思考序列号同步的作用',0,'string',NULL,NULL,25,7,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','solution','参考答案','可选，编程题参考代码片段','function twoSum(nums, target) { ... }',0,'string',NULL,NULL,40,8,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','status','状态','可选，默认published','published',0,'dict',NULL,'draft,published,archived',14,9,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','sort','排序','可选，整数，越小越靠前','1',0,'number',NULL,NULL,10,10,'0','admin','2026-08-20 13:21:05','',NULL,NULL);
INSERT INTO `moyun-db`.portal_import_template_config (business_key,field_name,column_name,description,example_value,required,field_type,dict_type,combo_values,column_width,sort,status,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                                                                                                ('interview_question','questionType','题目类型','可选，下拉选择内容分类','bagwen',0,'dict',NULL,'bagwen,algorithm,system_design,project,hr',16,11,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','examinePoints','考察点','可选，JSON数组字符串','["TCP三次握手","序列号同步"]',0,'string',NULL,NULL,30,12,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','answerOutline','答题大纲','可选，Markdown格式答题思路','1. 建立连接
2. 同步序列号
3. 确认应答',0,'string',NULL,NULL,35,13,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','scoringCriteria','评分标准','可选，JSON数组字符串','[{"dimension":"准确性","weight":60}]',0,'string',NULL,NULL,30,14,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','referenceAnswer','官方参考答案','可选，Markdown格式完整答案（八股/设计/项目/HR类）','三次握手的核心目的是同步双方的初始序列号...',0,'string',NULL,NULL,40,15,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','prerequisiteIds','前置题目ID','可选，逗号分隔的题目ID','1,2,3',0,'string',NULL,NULL,16,16,'0','admin','2026-08-20 13:21:05','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','practiceMode','练习模式','可选，默认reading，下拉选择作答方式','choice',0,'dict',NULL,'reading,choice,coding',14,17,'0','admin','2026-08-20 13:21:06','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','options','选择题选项','practiceMode=choice时必填，JSON数组格式','[{"label":"A","text":"建立网络路由","is_correct":false},{"label":"B","text":"同步双方的初始序列号","is_correct":true}]',0,'string',NULL,NULL,50,18,'0','admin','2026-08-20 13:21:06','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','correctAnswer','正确答案','可选，选择题填选项label（如B），编程题留空','B',0,'string',NULL,NULL,12,19,'0','admin','2026-08-20 13:21:06','',NULL,NULL),
                                                                                                                                                                                                                                                ('interview_question','analysis','题目解析','可选，做题后展示的解析说明','三次握手的核心目的是让通信双方同步初始序列号（ISN）...',0,'string',NULL,NULL,40,20,'0','admin','2026-08-20 13:21:06','',NULL,NULL);
INSERT INTO `moyun-db`.portal_import_template_config (business_key,field_name,column_name,description,example_value,required,field_type,dict_type,combo_values,column_width,sort,status,create_by,create_time,update_by,update_time,remark) VALUES
    ('interview_question','knowledgeTags','知识点标签','可选，逗号分隔，用于错题本/学习路径聚类','TCP,三次握手,序列号,可靠传输',0,'string',NULL,NULL,25,21,'0','admin','2026-08-20 13:21:06','',NULL,NULL);
INSERT INTO `moyun-db`.portal_interview_attempt (question_id,user_id,attempt_count,last_attempt_at,status,first_solved_at,last_solved_at,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                          (1,7,3,'2026-08-28 13:44:24','solved','2026-08-28 13:44:02','2026-08-28 13:44:24','','2026-08-28 13:44:02','','2026-08-28 13:44:24',NULL,'0'),
                                                                                                                                                                                                          (5,7,1,'2026-08-28 13:44:45','solved','2026-08-28 13:44:45','2026-08-28 13:44:45','','2026-08-28 13:44:44','','2026-08-28 13:44:44',NULL,'0'),
                                                                                                                                                                                                          (1,6,2,'2026-09-07 17:08:33','solved','2026-09-07 17:08:33','2026-09-07 17:08:33','','2026-08-28 16:27:46','','2026-09-07 17:08:33',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_bookmark (question_id,user_id,note,create_time,create_by,update_by,update_time,remark,del_flag) VALUES
    (8,6,NULL,'2026-08-31 10:15:47','','','2026-08-31 10:15:47',NULL,'1');
INSERT INTO `moyun-db`.portal_interview_category (name,slug,description,icon,sort,question_count,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                         ('算法与数据结构','algorithm','算法题、数据结构相关面试题','fa-code',1,150,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
                                                                                                                                                                         ('系统设计','system-design','系统架构设计、分布式系统等面试题','fa-sitemap',2,60,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
                                                                                                                                                                         ('前端开发','frontend','JavaScript、CSS、Vue、React等前端技术面试题','fa-laptop-code',3,120,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
                                                                                                                                                                         ('后端开发','backend','Java、Python、Go等后端技术面试题','fa-server',4,130,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
                                                                                                                                                                         ('数据库','database','MySQL、Redis等数据库相关面试题','fa-database',5,80,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_comment (experience_id,user_id,parent_id,reply_to_user_id,content,like_count,status,auditor_id,audit_remark,audit_time,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                (1,6,NULL,NULL,'愤怒的是说的都是',0,'published',NULL,NULL,NULL,'','2026-08-31 10:43:40','','2026-08-31 10:43:40',NULL,'0'),
                                                                                                                                                                                                                                (1,6,NULL,NULL,'啊大',1,'published',NULL,NULL,NULL,'','2026-08-31 10:43:44','','2026-08-31 10:54:16',NULL,'0'),
                                                                                                                                                                                                                                (1,6,NULL,NULL,'俺的阿莎噶',0,'published',NULL,NULL,NULL,'','2026-08-31 10:43:48','','2026-08-31 10:43:48',NULL,'0'),
                                                                                                                                                                                                                                (1,6,NULL,NULL,'啊大',1,'published',NULL,NULL,NULL,'','2026-08-31 10:43:53','','2026-09-02 13:39:12',NULL,'0'),
                                                                                                                                                                                                                                (1,6,1,NULL,'阿达v',0,'published',NULL,NULL,NULL,'','2026-08-31 10:54:01','','2026-08-31 10:54:01',NULL,'0'),
                                                                                                                                                                                                                                (1,6,2,NULL,'啊发撒啊撒',0,'published',NULL,NULL,NULL,'','2026-08-31 10:54:15','','2026-08-31 10:54:15',NULL,'0'),
                                                                                                                                                                                                                                (1,6,2,NULL,'1111111111111111',0,'published',NULL,NULL,NULL,'','2026-08-31 10:54:44','','2026-08-31 10:54:44',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_comment_like (comment_id,user_id,create_time) VALUES
                                                                                          (2,6,'2026-08-31 10:54:16'),
                                                                                          (4,6,'2026-09-02 13:39:12');
INSERT INTO `moyun-db`.portal_interview_config (config_name,persona_type,prompt_template,scoring_weights,question_weights,max_followups,followup_triggers,enable_self_intro,self_intro_duration,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
    ('默认面试配置','professional','','{"selfIntro":{"structure":30,"awareness":25,"matching":25,"fluency":20},"llmRatio":70,"total":{"intro":20,"tech":80}}','{"job":40,"resume":30,"weak":20,"random":10}',4,'["vague_answer","contradiction","depth_needed"]',1,180,1,'active','admin','2026-09-07 16:37:36','','2026-09-07 16:42:04','系统默认：llmRatio=LLM融合比例(0-100)；selfIntro=自我介绍4维权重；total=总分权重(intro/tech)；关闭自我介绍以兼容旧流程','0');
INSERT INTO `moyun-db`.portal_interview_experience (user_id,title,company,`position`,`year`,`month`,summary,content,cover_image,tags,is_top,view_count,like_count,comment_count,status,auditor_id,audit_remark,audit_time,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
    (6,'java面试','字节跳动','java',2026,8,'面试','阿班阿布
-- ==================== 增量变更：简历模板套用打通 + 评分报告存档（2026-08-31） ====================
-- 设计文档：docs/简历编辑和优化模块重构设计-20260826.md 阶段一/阶段五
-- 1) 模板表增加结构化示例数据字段（可选，便于"基于此模板创建简历"快速填充表单）
ALTER TABLE `portal_interview_resume_template`
  ADD COLUMN `sample_data` json DEFAULT NULL COMMENT ''模板结构化示例数据（JSON：name/phone/email/jobIntention/educations/works/projects/skills/selfIntro，用于一键套用填充编辑页）'' AFTER `tags`;

-- 2) 评分报告表：保存每次评分结果为可追溯报告（含岗位快照与各维度明细）
CREATE TABLE `portal_resume_score_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''报告ID'',
  `user_id` bigint NOT NULL COMMENT ''用户ID（门户用户ID）'',
  `resume_id` bigint NOT NULL COMMENT ''简历ID（portal_user_resume.id）'',
  `job_target_id` bigint DEFAULT NULL COMMENT ''关联岗位目标ID（可选，纯规则评分时为空）'',
  `position_snapshot` varchar(100) DEFAULT NULL COMMENT ''评分时的目标岗位快照（便于报告独立解读）'',
  `score` int NOT NULL COMMENT ''综合评分 0-100'',
  `score_detail` text COMMENT ''各维度评分明细 JSON（基本信息/求职意向/教育/工作/项目/技能/自我评价/岗位匹配度）'',
  `source` varchar(20) DEFAULT ''manual'' COMMENT ''评分来源：manual 单独评分 / optimize 优化后重新评分 / template 模板套用评分'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''评分时间'',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT=''简历优化-评分报告存档表'';
',NULL,'java springboot',0,13,1,7,'published',1,'好的','2026-08-31 10:42:47','','2026-08-31 10:42:06','','2026-09-07 09:19:21',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_experience_like (experience_id,user_id,create_time,create_by,update_by,update_time,remark) VALUES
    (1,6,'2026-08-31 10:44:43','','','2026-08-31 10:44:42',NULL);
INSERT INTO `moyun-db`.portal_interview_position (code,name,industry,`level`,required_skills,hot_companies,description,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                    ('java_backend','Java后端工程师','互联网','mid','["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]','["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]','Java 后端工程师岗位，重点考察 Java 基础、Spring 全家桶、MySQL/Redis、分布式与微服务、JVM 与并发编程',1,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),
                                                                                                                                                                                                    ('frontend','前端工程师','互联网','mid','["JavaScript","TypeScript","Vue","React","HTML","CSS","Node.js","Webpack","Vite","性能优化","浏览器原理","HTTP"]','["阿里","腾讯","字节跳动","美团","京东","百度","网易","小米","Shopee","滴滴"]','前端工程师岗位，重点考察 JS/TS 基础、Vue/React 框架、工程化、浏览器原理、性能优化、HTTP 与网络',2,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),
                                                                                                                                                                                                    ('algorithm','算法工程师','互联网','mid','["算法","数据结构","动态规划","图论","字符串","数组","链表","树","递归","排序","机器学习","深度学习","数学"]','["阿里","腾讯","字节跳动","百度","美团","快手","小红书","华为","商汤","旷视"]','算法工程师岗位，重点考察数据结构与算法、动态规划、图论、字符串算法、机器学习与深度学习基础',3,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_question (title,description,difficulty,category_id,job_template_id,tags,companies,acceptance_rate,submission_count,like_count,hint,solution,sort,status,question_type,examine_points,answer_outline,scoring_criteria,reference_answer,prerequisite_ids,practice_mode,`options`,correct_answer,analysis,knowledge_tags,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                                                                                                                               ('TCP三次握手的目的是什么？','TCP协议建立连接时使用三次握手，请选择其主要目的。','easy',NULL,NULL,'TCP,网络,三次握手',NULL,60.00,125,18,'思考序列号同步的作用',NULL,1,'published','bagwen',NULL,NULL,NULL,NULL,NULL,'choice','[{"text": "建立网络路由", "label": "A", "is_correct": false}, {"text": "同步双方的初始序列号", "label": "B", "is_correct": true}, {"text": "加密传输数据", "label": "C", "is_correct": false}, {"text": "压缩数据包", "label": "D", "is_correct": false}]','B','三次握手的核心目的是让通信双方同步初始序列号（ISN），确保后续数据传输的有序性和可靠性。序列号用于确认应答和重传控制，是 TCP 可靠传输的基础。','TCP,三次握手,序列号,可靠传输','admin','2026-08-20 10:56:16','','2026-08-20 10:56:16',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                               ('HTTP与HTTPS的主要区别是什么？','HTTPS 相比 HTTP 增加了安全性，请选择其核心区别。','easy',NULL,NULL,'HTTP,HTTPS,SSL,TLS',NULL,78.30,95,12,'思考加密层的作用',NULL,2,'published','bagwen',NULL,NULL,NULL,NULL,NULL,'choice','[{"text": "HTTPS使用了不同的端口", "label": "A", "is_correct": false}, {"text": "HTTPS加入了SSL/TLS加密层", "label": "B", "is_correct": true}, {"text": "HTTPS传输速度更快", "label": "C", "is_correct": false}, {"text": "HTTPS不需要证书", "label": "D", "is_correct": false}]','B','HTTPS 在 HTTP 基础上加入了 SSL/TLS 协议层，对传输内容进行加密，保证数据机密性和完整性。HTTP 默认端口 80，HTTPS 默认端口 443。HTTPS 需要CA证书。','HTTP,HTTPS,SSL,TLS,加密','admin','2026-08-20 10:56:16','','2026-08-20 10:56:16',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                               ('进程和线程的本质区别是什么？','进程和线程是操作系统的核心概念，请选择其本质区别。','medium',NULL,NULL,'进程,线程,操作系统',NULL,65.20,88,15,'思考资源分配单位',NULL,3,'published','bagwen',NULL,NULL,NULL,NULL,NULL,'choice','[{"text": "线程是进程内的执行单元，共享进程资源", "label": "A", "is_correct": true}, {"text": "进程比线程轻量", "label": "B", "is_correct": false}, {"text": "线程不能独立存在", "label": "C", "is_correct": false}, {"text": "进程间通信比线程间快", "label": "D", "is_correct": false}]','A','进程是资源分配的最小单位，线程是CPU调度的最小单位。线程存在于进程内，共享进程的内存空间和资源，线程间通信成本低于进程间通信，但需要同步机制保证数据一致性。','进程,线程,资源分配,CPU调度','admin','2026-08-20 10:56:17','','2026-08-20 10:56:17',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                               ('以下哪种情况会产生死锁？','死锁是并发编程的常见问题，请选择产生死锁的必要条件组合。','medium',NULL,NULL,'死锁,并发,操作系统',NULL,58.40,72,10,'思考死锁的四个必要条件',NULL,4,'published','bagwen',NULL,NULL,NULL,NULL,NULL,'choice','[{"text": "互斥、占有并等待、不剥夺、循环等待", "label": "A", "is_correct": true}, {"text": "共享、抢占、可剥夺、线性等待", "label": "B", "is_correct": false}, {"text": "互斥、释放、剥夺、无等待", "label": "C", "is_correct": false}, {"text": "并发、并行、同步、异步", "label": "D", "is_correct": false}]','A','死锁产生的四个必要条件：互斥条件、占有并等待、不剥夺条件、循环等待条件。只有四个条件同时满足才会发生死锁，破坏任一条件即可预防死锁。','死锁,并发,互斥,资源竞争','admin','2026-08-20 10:56:17','','2026-08-20 10:56:17',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                               ('Redis中以下哪种数据类型不支持？','Redis 提供多种数据结构，请选择不支持的类型。','medium',NULL,NULL,'Redis,数据结构,缓存',NULL,100.00,66,8,'回顾Redis五大基本类型',NULL,5,'published','bagwen',NULL,NULL,NULL,NULL,NULL,'choice','[{"text": "String 字符串", "label": "A", "is_correct": false}, {"text": "Hash 哈希表", "label": "B", "is_correct": false}, {"text": "Tree 树结构", "label": "C", "is_correct": true}, {"text": "Set 集合", "label": "D", "is_correct": false}]','C','Redis 五大基本数据类型：String（字符串）、Hash（哈希表）、List（列表）、Set（集合）、ZSet（有序集合）。Tree 不是 Redis 的基本类型（Redis 内部用跳表实现 ZSet，但不直接暴露 Tree 结构）。','Redis,数据结构,缓存,NoSQL','admin','2026-08-20 10:56:17','','2026-08-20 10:56:17',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                               ('两数之和','给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值的那两个整数，并返回它们的数组下标。

你可以假设每种输入只会对应一个答案。但是，数组中同一个元素不能在答案里重复出现。

你可以按任意顺序返回答案。

示例 1：
输入：nums = [2,7,11,15], target = 9
输出：[0,1]
解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。','easy',NULL,NULL,'数组,哈希表','字节跳动,腾讯,阿里',100.00,158,22,'考虑使用哈希表一次遍历','function twoSum(nums, target) {
  const map = new Map();
  for (let i = 0; i < nums.length; i++) {
    const complement = target - nums[i];
    if (map.has(complement)) return [map.get(complement), i];
    map.set(nums[i], i);
  }
}',10,'published','algorithm',NULL,NULL,NULL,NULL,NULL,'coding',NULL,NULL,'哈希表一次遍历解法：遍历数组时，对每个元素计算其补数（target - nums[i]），若补数已在哈希表中则直接返回两个下标，否则将当前元素存入哈希表。时间复杂度 O(n)，空间复杂度 O(n)。','数组,哈希表,一次遍历','admin','2026-08-20 10:56:17','','2026-08-20 10:56:17',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                               ('反转链表','给你单链表的头节点 head，请你反转链表，并返回反转后的链表。

示例：
输入：head = [1,2,3,4,5]
输出：[5,4,3,2,1]','easy',NULL,NULL,'链表,指针','美团,字节跳动',0.00,104,15,'考虑迭代或递归两种解法','function reverseList(head) {
  let prev = null, curr = head;
  while (curr) {
    const next = curr.next;
    curr.next = prev;
    prev = curr;
    curr = next;
  }
  return prev;
}',11,'published','algorithm',NULL,NULL,NULL,NULL,NULL,'coding',NULL,NULL,'迭代解法：使用三个指针 prev/curr/next，逐个节点反转指向。时间复杂度 O(n)，空间复杂度 O(1)。递归解法也可，但空间复杂度为 O(n)（递归栈）。','链表,指针,迭代','admin','2026-08-20 10:56:17','','2026-08-20 10:56:17',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                                                               ('有效的括号','给定一个只包括 (、)、{、}、[、] 的字符串 s，判断字符串是否有效。

有效字符串需满足：
1. 左括号必须用相同类型的右括号闭合。
2. 左括号必须以正确的顺序闭合。

示例：
输入：s = "()[]{}"
输出：true
输入：s = "(]"
输出：false','medium',NULL,NULL,'栈,字符串','字节跳动,腾讯',44.80,110,19,'考虑使用栈结构','function isValid(s) {
  const stack = [];
  const map = {")":"(", "]":"[", "}":"{"};
  for (const c of s) {
    if (c in map) {
      if (stack.pop() !== map[c]) return false;
    } else stack.push(c);
  }
  return stack.length === 0;
}',12,'published','algorithm',NULL,NULL,NULL,NULL,NULL,'coding',NULL,NULL,'栈解法：遍历字符串，遇到左括号入栈，遇到右括号弹出栈顶判断是否匹配。最终栈为空则有效。时间复杂度 O(n)，空间复杂度 O(n)。','栈,字符串,匹配','admin','2026-08-20 10:56:17','','2026-08-31 10:15:43',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_question_like (question_id,user_id,create_time,create_by,update_by,update_time,remark) VALUES
    (8,6,'2026-08-31 10:15:43','','','2026-08-31 10:15:43',NULL);
INSERT INTO `moyun-db`.portal_interview_question_test_case (question_id,`input`,expected_output,is_sample,order_num,explanation,create_time,update_time) VALUES
                                                                                                                                                             (6,'[2,7,11,15]
9','[0,1]',1,1,'基本示例：2+7=9','2026-08-20 10:56:17','2026-08-20 10:56:17'),
                                                                                                                                                             (6,'[3,2,4]
6','[1,2]',1,2,'2+4=6','2026-08-20 10:56:17','2026-08-20 10:56:17'),
                                                                                                                                                             (6,'[3,3]
6','[0,1]',0,3,'相同值不同下标','2026-08-20 10:56:17','2026-08-20 10:56:17'),
                                                                                                                                                             (7,'[1,2,3,4,5]','[5,4,3,2,1]',1,1,'基本示例','2026-08-20 10:56:17','2026-08-20 10:56:17'),
                                                                                                                                                             (7,'[1,2]','[2,1]',0,2,'两个节点','2026-08-20 10:56:17','2026-08-20 10:56:17'),
                                                                                                                                                             (7,'[]','[]',0,3,'空链表','2026-08-20 10:56:17','2026-08-20 10:56:17'),
                                                                                                                                                             (8,'()[]{}','true',1,1,'基本有效示例','2026-08-20 10:56:17','2026-08-20 10:56:17'),
                                                                                                                                                             (8,'(]','false',1,2,'基本无效示例','2026-08-20 10:56:17','2026-08-20 10:56:17'),
                                                                                                                                                             (8,'([)]','false',0,3,'嵌套但不合法','2026-08-20 10:56:17','2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_resume_template (title,description,cover,download_url,category,file_type,file_size,is_premium,usage_guide,tags,sample_data,like_count,download_count,sort,status,create_by,create_time,update_by,update_time,preview_images,remark,del_flag) VALUES
    ('java高级简历模板','','http://127.0.0.1:9001/moyun/2026/08/31/d88a580a882f460e9f6f2e762097ae3f.png','http://127.0.0.1:9001/moyun/2026/08/31/df797bbb6ca044c1a06359cc08fe0e25.pdf',NULL,'pdf',NULL,0,NULL,'',NULL,0,0,0,'','','2026-08-31 09:20:51','','2026-09-07 16:21:17','["http://127.0.0.1:9001/moyun/2026/08/31/de169a8619444c9380cae4a20f5f9c50.png"]',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_submission (question_id,user_id,code,content,`language`,answer_type,status,is_success,runtime,memory_usage,note,is_featured,featured_time,passed_case_count,total_case_count,failed_case_id,failed_case_input,failed_case_expected,failed_case_actual,error_message,create_time,create_by,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                                                                             (6,7,'// ACM 模式：从标准输入读取，向标准输出写结果
// 输入格式：第1行数组，第2行目标值，如 "[2,7,11,15]" / "9"
// 输出格式：如下标对，如 "[0,1]"
const lines = require(''fs'').readFileSync(0, ''utf8'').split(''\\n'');
const nums = JSON.parse(lines[0]);
const target = Number(lines[1]);

const seen = new Map();
for (let i = 0; i < nums.length; i++) {
  const j = seen.get(target - nums[i]);
  if (j !== undefined) {
    console.log(JSON.stringify([j, i]));
    process.exit(0);
  }
  seen.set(nums[i], i);
}
console.log(JSON.stringify([]));',NULL,'javascript','code','AC',1,206,NULL,NULL,0,NULL,3,3,NULL,NULL,NULL,NULL,NULL,'2026-08-28 13:38:35','','','2026-08-28 13:38:34',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (6,7,'// ACM 模式：从标准输入读取，向标准输出写结果
// 输入格式：第1行数组，第2行目标值，如 "[2,7,11,15]" / "9"
// 输出格式：如下标对，如 "[0,1]"
const lines = require(''fs'').readFileSync(0, ''utf8'').split(''\\n'');
const nums = JSON.parse(lines[0]);
const target = Number(lines[1]);

const seen = new Map();
for (let i = 0; i < nums.length; i++) {
  const j = seen.get(target - nums[i]);
  if (j !== undefined) {
    console.log(JSON.stringify([j, i]));
    process.exit(0);
  }
  seen.set(nums[i], i);
}
console.log(JSON.stringify([]));',NULL,'javascript','code','AC',1,163,NULL,NULL,0,NULL,3,3,NULL,NULL,NULL,NULL,NULL,'2026-08-28 13:38:58','','','2026-08-28 13:38:57',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (7,7,'// ACM 模式：框架已自动注入 BufferedReader br 读取标准输入
// 直接在 main 体内写逻辑即可（无需定义类与方法）
String l1 = br.readLine();
String l2 = br.readLine();

String inner = l1.substring(1, l1.length() - 1);
String[] parts = inner.isEmpty() ? new String[0] : inner.split(",");
int[] nums = new int[parts.length];
for (int i = 0; i < parts.length; i++) {
    nums[i] = Integer.parseInt(parts[i].trim());
}
int target = Integer.parseInt(l2.trim());

java.util.Map<Integer, Integer> seen = new java.util.HashMap<>();
for (int i = 0; i < nums.length; i++) {
    Integer j = seen.get(target - nums[i]);
    if (j != null) {
        System.out.println("[" + j + "," + i + "]");
        return;
    }
    seen.put(nums[i], i);
}
System.out.println("[]");',NULL,'java','code','RE',0,354,NULL,NULL,0,NULL,0,3,4,'[1,2,3,4,5]','[5,4,3,2,1]','','[RE] 退出码 1','2026-08-28 13:40:03','','','2026-08-28 13:40:03',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (7,7,'// ACM 模式：框架已自动注入 BufferedReader br 读取标准输入
// 直接在 main 体内写逻辑即可（无需定义类与方法）
String l1 = br.readLine();
String l2 = br.readLine();

String inner = l1.substring(1, l1.length() - 1);
String[] parts = inner.isEmpty() ? new String[0] : inner.split(",");
int[] nums = new int[parts.length];
for (int i = 0; i < parts.length; i++) {
    nums[i] = Integer.parseInt(parts[i].trim());
}
int target = Integer.parseInt(l2.trim());

java.util.Map<Integer, Integer> seen = new java.util.HashMap<>();
for (int i = 0; i < nums.length; i++) {
    Integer j = seen.get(target - nums[i]);
    if (j != null) {
        System.out.println("[" + j + "," + i + "]");
        return;
    }
    seen.put(nums[i], i);
}
System.out.println("[]");',NULL,'java','code','RE',0,157,NULL,NULL,0,NULL,0,3,4,'[1,2,3,4,5]','[5,4,3,2,1]','','[RE] 退出码 1','2026-08-28 13:40:20','','','2026-08-28 13:40:20',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (7,7,'// ACM 模式：框架已自动注入 BufferedReader br 读取标准输入
// 直接在 main 体内写逻辑即可（无需定义类与方法）
String l1 = br.readLine();
String l2 = br.readLine();

String inner = l1.substring(1, l1.length() - 1);
String[] parts = inner.isEmpty() ? new String[0] : inner.split(",");
int[] nums = new int[parts.length];
for (int i = 0; i < parts.length; i++) {
    nums[i] = Integer.parseInt(parts[i].trim());
}
int target = Integer.parseInt(l2.trim());

java.util.Map<Integer, Integer> seen = new java.util.HashMap<>();
for (int i = 0; i < nums.length; i++) {
    Integer j = seen.get(target - nums[i]);
    if (j != null) {
        System.out.println("[" + j + "," + i + "]");
        return;
    }
    seen.put(nums[i], i);
}

System.out.println("[]");',NULL,'java','code','RE',0,183,NULL,NULL,0,NULL,0,3,4,'[1,2,3,4,5]','[5,4,3,2,1]','','[RE] 退出码 1','2026-08-28 13:41:04','','','2026-08-28 13:41:04',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (7,7,'System.out.println(123);',NULL,'java','code','WA',0,158,NULL,NULL,0,NULL,0,3,4,'[1,2,3,4,5]','[5,4,3,2,1]','123
',NULL,'2026-08-28 13:42:15','','','2026-08-28 13:42:14',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (7,7,'System.out.println("[5,4,3,2,1]");',NULL,'java','code','WA',0,188,NULL,NULL,0,NULL,1,3,5,NULL,NULL,NULL,NULL,'2026-08-28 13:42:55','','','2026-08-28 13:42:55',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (7,7,'System.out.println("[5,4,3,2,1]");',NULL,'java','code','WA',0,173,NULL,NULL,0,NULL,1,3,5,NULL,NULL,NULL,NULL,'2026-08-28 13:43:09','','','2026-08-28 13:43:09',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (1,7,NULL,'B','text','choice','accepted',1,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 13:44:02','','','2026-08-28 13:44:02',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (1,7,NULL,'A','text','choice','wrong_answer',0,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 13:44:19','','','2026-08-28 13:44:18',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_submission (question_id,user_id,code,content,`language`,answer_type,status,is_success,runtime,memory_usage,note,is_featured,featured_time,passed_case_count,total_case_count,failed_case_id,failed_case_input,failed_case_expected,failed_case_actual,error_message,create_time,create_by,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                                                                             (1,7,NULL,'B','text','choice','accepted',1,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 13:44:24','','','2026-08-28 13:44:24',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (5,7,NULL,'C','text','choice','accepted',1,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 13:44:45','','','2026-08-28 13:44:44',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (1,6,NULL,'A','text','choice','wrong_answer',0,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 16:27:47','','','2026-08-28 16:27:46',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                             (1,6,NULL,'B','text','choice','accepted',1,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-07 17:08:33','','','2026-09-07 17:08:33',NULL,'0');
INSERT INTO `moyun-db`.portal_job_template (name,category,position_code,description,jd_text,keywords,difficulty,question_count,weights,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                               ('Java后端工程师（示例）','技术','Java后端','Java 后端岗位面试模板示例','岗位职责：负责核心业务系统的设计与开发，参与需求评审、技术方案设计；任职要求：精通 Java 及 JVM 原理，熟悉 Spring Boot / MyBatis 生态，掌握 MySQL 索引优化与事务隔离级别，熟悉 Redis 缓存方案与消息队列（RocketMQ/Kafka），了解分布式系统设计（CAP/分布式锁/幂等），具备高并发场景调优经验。','Java,JVM,Spring Boot,MySQL,Redis,消息队列,分布式,高并发','medium',5,'{"job":40,"resume":30,"weak":20,"random":10}','active','admin','2026-09-07 15:07:33','',NULL,'示例模板：可在后台编辑 JD 后点击「LLM 提取关键词」重新生成','0'),
                                                                                                                                                                                                               ('前端工程师（示例）','技术','前端','前端岗位面试模板示例','岗位职责：负责 Web 前端功能开发与性能优化；任职要求：熟练掌握 JavaScript/TypeScript、Vue3 组合式 API 与响应式原理，理解浏览器渲染流程与事件循环，掌握 Webpack/Vite 构建优化，熟悉 HTTP 协议与前端安全（XSS/CSRF），有小程序/跨端经验者优先。','JavaScript,TypeScript,Vue3,浏览器渲染,事件循环,Webpack,HTTP,XSS','medium',5,'{"job":40,"resume":30,"weak":20,"random":10}','active','admin','2026-09-07 15:07:33','',NULL,'示例模板：可在后台编辑 JD 后点击「LLM 提取关键词」重新生成','0');
INSERT INTO `moyun-db`.portal_like (user_id,article_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                               (7,9,'','2026-08-28 13:31:05','','2026-08-28 13:31:05',NULL),
                                                                                                               (6,2,'','2026-08-28 13:53:48','','2026-08-28 13:53:47',NULL);
INSERT INTO `moyun-db`.portal_resume_job_match (user_id,resume_id,job_target_id,match_score,grade,matched_keywords,missing_keywords,dimensions,summary,ai_powered,create_time) VALUES
                                                                                                                                                                                   (6,1,1,5,'poor',NULL,'Java','{"skillMatch": {"score": 0, "suggestions": ["补充Java基础、面向对象、集合、并发等技能", "补充常用Java生态技术，如Spring、Spring Boot、MySQL、Git等"]}, "keywordMatch": {"score": 0, "suggestions": ["在技能、项目或工作经历中明确补充Java相关关键词", "围绕Java工程师岗位增加后端开发相关术语"]}, "structureMatch": {"score": 10, "suggestions": ["补充电话、求职意向、教育经历、工作经历、项目经历、技能列表和自我介绍", "完善简历各模块内容，形成完整可评估的求职材料"]}, "experienceMatch": {"score": 0, "suggestions": ["补充Java相关工作经历或实习经历", "补充可体现Java开发能力的项目经历"]}}','候选人简历除姓名和邮箱外几乎为空，未体现任何Java技能、项目或工作经验，与Java工程师岗位匹配度极低。建议优先补充求职意向、教育经历、Java相关技能与项目/工作经历，并增加可量化的成果描述。',1,'2026-08-31 10:37:49'),
                                                                                                                                                                                   (6,1,2,3,'poor',NULL,'Java、后端框架、HTML、CSS、JavaScript、Vue、MySQL、数据库优化、微服务、系统架构、Web应用、高可用、代码审查、敏捷开发、沟通协作、独立解决问题、学习能力、本科、视觉算法、深度学习','{"skillMatch": {"score": 0, "suggestions": ["技能列表为空，无法评估岗位所需技术能力", "建议补充Java及后端框架、前端Vue、MySQL、微服务架构等技能", "如有敏捷开发、团队协作、独立排障等软技能，也应具体说明"]}, "keywordMatch": {"score": 0, "suggestions": ["简历未体现Java、后端框架、前端技术等岗位核心关键词", "如有相关技术基础，应补充HTML、CSS、JavaScript、Vue、MySQL等关键词", "若有视觉算法或深度学习经验，应单独明确写出"]}, "structureMatch": {"score": 8, "suggestions": ["简历仅有姓名和邮箱，电话等基础信息不完整", "缺少教育经历、求职意向、工作经历、项目经历、技能列表和自我介绍", "建议按目标岗位补齐核心模块，并量化描述项目成果"]}, "experienceMatch": {"score": 0, "suggestions": ["简历缺少工作经历、项目经历，无法判断Web应用开发经验", "建议补充全栈开发、系统架构、数据库设计、微服务等实际项目经验", "可补充高可用、性能优化、代码审查、故障排查等职责相关经验"]}}','候选人简历信息严重不足，未体现全栈工程师所需的Java、前端、数据库、微服务和项目经验，当前与岗位匹配度很低。建议优先补齐教育、工作、项目、技能等模块，并围绕Java、Vue、MySQL、Web应用和高可用开发进行针对性描述。',1,'2026-09-01 09:05:20'),
                                                                                                                                                                                   (6,1,2,3,'poor',NULL,'Java、后端框架、HTML、CSS、JavaScript、Vue、MySQL、数据库优化、微服务架构、Web应用开发、系统架构设计、代码审查、高可用、团队协作、敏捷开发、独立解决问题、学习能力、本科及以上、深度学习、视觉算法','{"skillMatch": {"score": 0, "suggestions": ["技能列表为空，无法证明Java、Vue、MySQL、微服务等岗位所需能力。", "建议按后端、前端、数据库、架构与工程能力分类补充技能，并标注熟练程度。"]}, "keywordMatch": {"score": 0, "suggestions": ["简历中未出现Java、后端框架、前端技术、数据库、微服务等岗位核心关键词。", "建议在技能、项目和工作经历中明确写出与全栈工程师岗位相关的技术关键词。"]}, "structureMatch": {"score": 10, "suggestions": ["简历仅包含姓名和邮箱，教育经历、求职意向、工作经历、项目经历、技能列表和自我介绍均缺失。", "建议先补齐基础简历模块，再针对目标岗位进行内容优化。"]}, "experienceMatch": {"score": 0, "suggestions": ["未填写工作经历和项目经历，无法评估Web应用开发、系统架构、数据库设计等经验。", "建议补充完整项目背景、个人职责、技术栈和可量化成果。"]}}','候选人与全栈工程师岗位当前匹配度极低，简历缺少教育、工作、项目、技能和自我介绍等关键信息，无法证明Java、前端、数据库及全栈开发能力。建议优先补齐简历核心模块，并围绕Java后端、Vue前端、MySQL、微服务和Web应用开发要求补充可验证的项目成果。',1,'2026-09-01 09:48:41'),
                                                                                                                                                                                   (6,1,2,60,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库设计与优化、后端开发、前端开发、接口设计、代码评审、敏捷开发、与产品协作、与UI/UX协作、系统故障排查、高可用优化','本科及以上学历、计算机或软件工程专业、视觉算法、深度学习、项目经历、工作经历、高性能架构、系统架构设计、完整开发生命周期、独立解决复杂问题','{"skillMatch": {"score": 82, "suggestions": ["补充Spring Boot、MySQL、Vue等技能的具体使用场景或版本信息", "增加微服务落地细节，如服务拆分、网关、注册中心、链路追踪等", "补充测试、部署、CI/CD、性能优化等工程化能力"]}, "keywordMatch": {"score": 80, "suggestions": ["补充视觉算法或深度学习相关关键词", "明确本科及以上学历及计算机、软件工程专业背景", "增加高性能、可扩展、高可用等架构类关键词"]}, "structureMatch": {"score": 22, "suggestions": ["补全电话、求职意向、期望城市、期望薪资等基本信息", "补充教育经历，明确学历和专业", "补充工作经历、项目经历模块，形成完整简历结构"]}, "experienceMatch": {"score": 30, "suggestions": ["补充完整工作经历与项目经历，验证全栈开发经验", "将自我介绍中的[X]年、[X%]替换为真实量化数据", "补充具体业务场景、职责分工和交付成果"]}}','候选人技术栈与全栈工程师岗位核心要求较匹配，覆盖Java、Spring Boot、MySQL、Vue、微服务等关键能力。但简历缺少教育经历、工作经历和项目经历，且存在占位符，难以充分验证真实经验、架构能力和交付成果。建议补全项目案例、学历信息、量化指标，并补充视觉算法或深度学习相关经历以提升竞争力。',1,'2026-09-01 09:52:26'),
                                                                                                                                                                                   (6,1,2,55,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库优化、代码评审、敏捷协作、故障定位','视觉算法、深度学习、本科及以上学历、系统架构设计、高性能优化、Web应用安全、项目经历、工作经历','{"skillMatch": {"score": 75, "suggestions": ["已有Java/Spring Boot/MySQL/Vue等核心技能，建议用项目成果佐证熟练度", "补充前端工程化、接口规范、性能压测、安全实践等技能细节", "若有深度学习/视觉算法相关实践，单独列出技术栈与应用场景"]}, "keywordMatch": {"score": 72, "suggestions": ["补充视觉算法、深度学习等优先项关键词（如有）", "在经历中体现系统架构设计、高性能、安全、稳定性等岗位关键词", "将[X]年、[X%]等占位符替换为真实量化数据"]}, "structureMatch": {"score": 20, "suggestions": ["补全电话、求职意向、教育经历、工作经历、项目经历等关键模块", "为每段经历添加背景、职责、技术栈和量化成果", "删除占位符内容，形成可验证的完整简历"]}, "experienceMatch": {"score": 25, "suggestions": ["补充完整工作经历与项目经历，证明Java Web与全栈开发经验", "提供数据库设计、微服务拆分、接口设计、故障优化的具体案例和结果", "补充学历信息以满足本科及以上要求"]}}','候选人技能栈与全栈工程师岗位核心要求较匹配，覆盖Java、Spring Boot、MySQL、Vue、微服务和数据库优化等关键点。但简历缺少教育、工作/项目经历及量化成果，经验可信度和结构完整度明显不足。建议补齐学历与项目经历，突出系统架构、性能优化、高可用与安全实践，并补充视觉算法/深度学习相关加分项。',1,'2026-09-01 10:39:00'),
                                                                                                                                                                                   (6,1,2,45,'poor','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库优化、代码评审、敏捷协作、故障定位','视觉算法、深度学习、项目经历、工作经历、本科及以上学历、系统架构设计、高性能优化、量化成果','{"skillMatch": {"score": 70, "suggestions": ["现有Java、Spring Boot、Vue、MySQL、微服务技能与岗位匹配，但需通过项目证明熟练度。", "补充前后端工程化、部署运维、安全与性能相关技能。", "如有视觉算法或深度学习技术栈，请补充；否则可弱化优先项。"]}, "keywordMatch": {"score": 75, "suggestions": ["补充视觉算法、深度学习等优先项关键词，若有相关经验需明确写出。", "将技能关键词与具体项目结合，避免仅有技能列表。", "用量化指标强化高可用、性能优化等关键词。"]}, "structureMatch": {"score": 15, "suggestions": ["补全电话、求职意向、教育经历、工作经历、项目经历等必填模块。", "移除自我介绍中的[X]占位符，使用真实年限和量化结果。", "增加项目描述中的背景、职责、技术方案和成果。"]}, "experienceMatch": {"score": 20, "suggestions": ["补充完整工作经历与项目经历，明确Java全栈开发年限、业务规模和职责。", "提供系统架构设计、微服务落地、数据库优化的实际案例。", "补充教育经历以满足本科及以上要求。"]}}','候选人技能关键词与全栈岗位较匹配，覆盖Java、Spring Boot、Vue、MySQL和微服务等核心要求。但简历缺少教育、工作、项目经历及量化成果，无法证明岗位所需的丰富经验、架构能力和高可用交付能力。建议补全经历与项目案例，并补充视觉算法或深度学习相关经验。',1,'2026-09-01 10:39:57'),
                                                                                                                                                                                   (6,1,2,56,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库设计、查询优化、代码评审、敏捷协作、系统故障处理','视觉算法、深度学习、本科及以上学历、项目经历、工作经历、高可用架构、性能优化量化','{"skillMatch": {"score": 82, "suggestions": ["技能栈与岗位要求较匹配，覆盖后端、前端、数据库和微服务", "建议补充Spring Boot、Vue、MySQL、微服务在实际项目中的应用场景", "如有视觉算法、深度学习相关技术栈，应单独列出以提升岗位匹配度"]}, "keywordMatch": {"score": 72, "suggestions": ["简历已覆盖Java、Spring Boot、MySQL、Vue、微服务等核心关键词", "缺少视觉算法、深度学习等加分项关键词，若有相关经验建议补充", "建议补充高可用、可扩展架构、性能优化等岗位关键词的具体描述"]}, "structureMatch": {"score": 25, "suggestions": ["教育经历、工作经历、项目经历均为空，简历完整度不足", "缺少电话、求职意向等基础信息，影响招聘方联系和岗位匹配判断", "建议补全基本信息、教育背景、工作/项目经历，并增加可量化的成果描述"]}, "experienceMatch": {"score": 30, "suggestions": ["简历未填写工作经历和项目经历，无法验证全栈开发经验的真实性与深度", "自我介绍中存在[X]年、[X%]等占位符，需替换为真实年限和量化结果", "建议补充Web应用开发、微服务拆分、数据库优化、故障处理相关项目案例"]}}','候选人技能栈与全栈工程师岗位较匹配，覆盖Java、Spring Boot、MySQL、Vue和微服务等核心要求，但简历缺少教育经历、工作/项目经历及量化成果，经验可信度不足。建议补全项目经历和工作经历，明确开发年限、性能优化和高可用成果，并补充视觉算法或深度学习相关经验以提升匹配度。',1,'2026-09-01 10:41:05'),
                                                                                                                                                                                   (6,1,2,58,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务、数据库优化、代码评审、敏捷开发、系统故障定位、高可用、跨团队协作','视觉算法、深度学习、系统架构设计、完整项目经历、本科及以上学历、性能优化成果','{"skillMatch": {"score": 82, "suggestions": ["补充Spring Boot、微服务相关组件与部署运维能力", "补充Vue项目中的组件化、状态管理或前端工程化实践", "说明数据库设计、SQL优化、代码评审的具体案例"]}, "keywordMatch": {"score": 78, "suggestions": ["补充视觉算法、深度学习等岗位优先项关键词", "增加系统架构设计、完整开发生命周期等职责关键词", "将微服务、数据库优化、高可用等关键词与具体项目场景结合"]}, "structureMatch": {"score": 25, "suggestions": ["补齐手机号、求职意向、教育经历和工作经历", "增加项目经历模块并按STAR方式描述", "将自我介绍中的占位符替换为真实年限和数据"]}, "experienceMatch": {"score": 30, "suggestions": ["补充至少2-3个完整项目或工作经历", "说明项目规模、个人职责、技术栈和交付结果", "量化系统故障处理、高可用保障和性能优化成果"]}}','候选人在Java、Spring Boot、MySQL、Vue和微服务等全栈核心技术上覆盖较高，但简历缺少教育、工作/项目经历与量化成果，难以充分验证真实经验深度。建议补全项目职责、系统架构设计和高可用/性能优化成果，并明确学历及视觉算法/深度学习相关经历。',1,'2026-09-01 10:51:40'),
                                                                                                                                                                                   (6,1,2,58,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库优化、前端开发、后端开发、敏捷开发、代码评审、团队协作','视觉算法、深度学习、系统架构设计、本科及以上、工作经历、项目经历','{"skillMatch": {"score": 78, "suggestions": ["现有Java、Spring Boot、MySQL、Vue等技能与岗位匹配度较高", "可补充视觉算法或深度学习相关技能，提升岗位加分项"]}, "keywordMatch": {"score": 80, "suggestions": ["补充视觉算法、深度学习等岗位优先项相关关键词", "增加系统架构设计、高性能优化、高可用保障等职责关键词"]}, "structureMatch": {"score": 20, "suggestions": ["补充教育经历、工作经历、项目经历和求职意向", "完善联系电话并将自我介绍中的占位符替换为真实量化数据"]}, "experienceMatch": {"score": 30, "suggestions": ["补充完整工作经历和项目经历，证明全栈开发经验", "用具体案例说明数据库优化、微服务拆分、故障排查和高可用成果"]}}','候选人具备Java、Spring Boot、MySQL、Vue等全栈岗位核心技能，关键词覆盖较好，但简历缺少教育、工作、项目经历等关键证明材料，难以充分体现经验丰富的高级全栈要求。建议补全履历并量化系统架构、性能优化和高可用成果，同时补充视觉算法或深度学习相关经验以增强竞争力。',1,'2026-09-01 10:52:02'),
                                                                                                                                                                                   (6,1,2,42,'poor','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库优化、代码评审、系统故障排查、敏捷开发','视觉算法、深度学习、项目经历、工作经历、高可用架构、性能优化、系统架构设计、本科学历','{"skillMatch": {"score": 74, "suggestions": ["技能栈与岗位核心要求较匹配，但缺少技能在项目中的应用证据。", "建议补充Spring Boot、微服务组件、Vue页面开发、MySQL索引优化、接口设计等具体实践。", "若具备代码审查、CI/CD、测试、部署或视觉算法相关经验，应单独列出以增强竞争力。"]}, "keywordMatch": {"score": 80, "suggestions": ["已覆盖Java、前端、MySQL、微服务等核心关键词，建议在项目或工作经历中进一步用实例证明这些能力。", "如有视觉算法或深度学习经验，应补充相关关键词与项目成果；如无，可减少无关预期。", "将自我介绍中的占位符替换为真实年限、指标和业务场景，提高关键词可信度。"]}, "structureMatch": {"score": 12, "suggestions": ["教育经历、工作经历、项目经历和求职意向均缺失，简历完整度较低。", "补充电话、期望职位、期望城市、教育背景等基础信息，便于招聘方判断基本匹配。", "使用结构化表达呈现项目经历，如项目背景、职责、技术栈、难点、成果和量化指标。"]}, "experienceMatch": {"score": 12, "suggestions": ["简历缺少工作经历和项目经历，无法验证全栈工程师所需的完整开发生命周期经验。", "补充至少2-3个Web应用项目，说明架构设计、后端开发、前端实现、数据库优化和上线效果。", "增加高可用、高性能、故障排查和微服务落地的量化案例，例如QPS、响应时间、可用性提升等。"]}}','候选人在Java、Spring Boot、MySQL、Vue和微服务等核心技能关键词上与全栈工程师岗位有较高匹配度，但缺少教育、工作、项目和量化成果支撑，难以证明岗位要求的丰富实战经验。建议补齐完整简历结构，重点增加Web全栈项目经历、性能优化与高可用案例。若有视觉算法或深度学习经验，应明确突出；若无，则应聚焦工程能力证明。',1,'2026-09-01 14:33:36');
INSERT INTO `moyun-db`.portal_resume_job_match (user_id,resume_id,job_target_id,match_score,grade,matched_keywords,missing_keywords,dimensions,summary,ai_powered,create_time) VALUES
                                                                                                                                                                                   (6,1,2,55,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库优化、代码评审、敏捷开发、团队协作','视觉算法、深度学习、本科及以上、工作经历、项目经历、系统架构设计、高性能优化案例、可量化成果','{"skillMatch": {"score": 76, "suggestions": ["当前后端与前端技能与岗位较匹配，建议补充实际项目作品佐证", "若有视觉算法或深度学习相关实践，应单独列出", "补充架构设计、安全设计、性能调优相关能力证据"]}, "keywordMatch": {"score": 78, "suggestions": ["补充视觉算法、深度学习等岗位优先项关键词", "增加系统架构设计、高性能、安全性等JD核心词", "将微服务拆分、数据库优化等关键词放入具体项目描述中增强可信度"]}, "structureMatch": {"score": 22, "suggestions": ["补齐电话、求职意向、教育经历、工作经历和项目经历", "使用STAR格式描述项目背景、职责、技术方案和业务成果", "完善简历基本结构，提高招聘方可读性和可信度"]}, "experienceMatch": {"score": 28, "suggestions": ["补充完整工作经历和项目经历，证明实际参与过Web应用全生命周期开发", "提供高可用、故障定位、性能优化的量化成果", "避免使用[X]类占位符，明确工作年限与项目指标"]}}','候选人在Java、Spring Boot、Vue、MySQL、微服务等核心技能上与全栈工程师岗位匹配度较高，但简历缺少教育经历、工作经历、项目经历和量化成果，难以充分验证真实工程能力。建议补齐项目与工作经历，并增加视觉算法、深度学习等岗位加分项，以提升整体竞争力。',1,'2026-09-01 15:50:15'),
                                                                                                                                                                                   (6,1,2,68,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、前后端开发、接口设计、数据库设计、代码评审、敏捷协作、系统故障定位','本科及以上学历、系统架构设计、可扩展解决方案、安全与稳定性、视觉算法、深度学习','{"skillMatch": {"score": 86, "suggestions": ["Java、Spring Boot、MySQL、Vue、HTML/CSS/JavaScript与岗位核心技能匹配度较高", "可进一步补充系统设计、服务治理、性能调优、安全开发相关能力", "如有视觉算法或深度学习相关技术经历，应单独列出以匹配岗位优先项"]}, "keywordMatch": {"score": 84, "suggestions": ["已覆盖Java、Spring Boot、MySQL、Vue、前端基础和微服务等核心关键词", "补充岗位优先项关键词，如视觉算法、深度学习", "将工作经历中的[X]占位符替换为真实数据，提升关键词可信度"]}, "structureMatch": {"score": 35, "suggestions": ["基本信息缺少电话，求职意向未填写，影响简历完整度", "教育经历字段为空且描述无效，需补充学校、专业、学历和时间", "项目经历描述过于简单且含测试文字，应改写为背景、职责、技术栈和成果", "删除[X]、[X%]等占位符，替换为真实可量化的结果"]}, "experienceMatch": {"score": 58, "suggestions": ["具备Java后端与Vue前端协同开发经历，方向与全栈岗位相关", "缺少可验证的项目规模、用户量、性能指标和高可用成果", "教育经历未体现本科及以上要求，且时间信息异常，需要补齐真实学历背景", "建议补充架构设计、性能优化、故障治理等更具资深全栈工程师特征的实践"]}}','候选人在Java全栈技术栈上与岗位高度相关，覆盖Spring Boot、MySQL、Vue、微服务和前后端协作开发。但简历中教育信息缺失、项目描述不规范、多处占位符和时间异常，削弱了经验真实性与资深程度。建议补齐学历和量化项目成果，并增强架构设计、高可用及视觉算法/深度学习等加分项。',1,'2026-09-01 15:57:53'),
                                                                                                                                                                                   (6,1,2,62,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、数据库优化、代码评审、敏捷协作、故障定位','视觉算法、深度学习、本科及以上学历、计算机相关专业、高性能架构设计、独立解决复杂问题','{"skillMatch": {"score": 80, "suggestions": ["增加微服务治理、数据库调优、前端工程化等技能的具体实践证据", "若有视觉算法或深度学习经验，补充相关模型、框架和应用场景"]}, "keywordMatch": {"score": 78, "suggestions": ["保留并前置 Java、Spring Boot、MySQL、Vue 等岗位核心关键词", "如有相关经验，补充视觉算法、深度学习等优先项关键词"]}, "structureMatch": {"score": 35, "suggestions": ["补全电话、求职意向、教育经历等基础信息，删除乱码和无效描述", "按 STAR 结构重写工作经历和项目经历，明确时间线、角色、成果"]}, "experienceMatch": {"score": 55, "suggestions": ["补充可验证的全栈项目背景、业务规模、个人职责和上线结果", "量化高性能、可扩展、高可用相关成果，替换 [X]、[X%] 等占位符"]}}','候选人技术栈与全栈工程师岗位较为匹配，覆盖 Java、Spring Boot、MySQL、Vue 和微服务等核心要求，但简历信息完整性较差，项目与教育经历缺少有效支撑，难以证明资深交付能力。建议补全学历、项目细节和量化成果，并补充视觉算法、深度学习等加分项经历。',1,'2026-09-02 10:22:53'),
                                                                                                                                                                                   (6,1,2,68,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、SQL查询优化、接口设计、前后端联调、代码审查、敏捷开发、高可用、系统架构设计','本科及以上学历、深度学习、视觉算法、性能优化、系统安全性','{"skillMatch": {"score": 82, "suggestions": ["突出微服务拆分、服务治理、缓存、消息队列等后端深度能力", "补充前端工程化、组件化、复杂交互或性能优化细节"]}, "keywordMatch": {"score": 80, "suggestions": ["补充视觉算法、深度学习等岗位优先关键词", "增加性能优化、安全设计、高可用治理等结果型关键词"]}, "structureMatch": {"score": 40, "suggestions": ["删除占位符，补全学校、专业、学历、项目职责和链接", "使用STAR结构完善项目背景、职责、指标和成果"]}, "experienceMatch": {"score": 58, "suggestions": ["补全真实工作经历、项目时间线和教育经历", "补充可量化的业务成果、故障处理和系统优化案例"]}}','候选人技术栈与全栈工程师岗位高度相关，覆盖Java、Spring Boot、MySQL、Vue和微服务等核心要求。但简历存在明显占位符，学历、项目细节与量化成果不足，削弱经验可信度。建议补全教育经历与真实项目数据，并补充性能优化、安全、高可用及视觉算法/深度学习相关说明。',1,'2026-09-02 10:31:24'),
                                                                                                                                                                                   (6,1,2,68,'medium','Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、SQL优化、前后端联调、代码审查、敏捷协作','视觉算法、深度学习、系统架构设计、数据库模型设计、本科及以上学历信息、量化项目成果','{"skillMatch": {"score": 78, "suggestions": ["补充高性能、可扩展性、安全性和高可用方面的落地证据。", "若有视觉算法或深度学习实践，建议以项目形式呈现。"]}, "keywordMatch": {"score": 82, "suggestions": ["补充视觉算法或深度学习相关关键词与经历，贴合岗位优先项。", "在简历中明确系统架构设计、数据库模型设计等JD核心关键词。"]}, "structureMatch": {"score": 48, "suggestions": ["删除模板占位内容，补全教育经历、学校、专业、学历和时间。", "用STAR方式重写项目经历，增加量化指标和可验证链接。"]}, "experienceMatch": {"score": 60, "suggestions": ["补全工作经历与项目经历的真实职责、周期和成果，避免占位符。", "围绕Web应用开发、前后端协作和故障处理提供具体案例。"]}}','候选人在Java、Spring Boot、MySQL、Vue和微服务等核心技术栈上与全栈工程师岗位高度匹配，但简历模板化严重，教育、项目和量化成果信息不足。建议补全真实经历与学历信息，并强化架构设计、高可用及视觉算法/深度学习相关证明。',1,'2026-09-02 10:31:56'),
                                                                                                                                                                                   (6,1,2,66,'medium','全栈工程师、Java、Spring Boot、MySQL、Vue、HTML、CSS、JavaScript、微服务架构、SQL查询优化、接口设计、前后端联调、产品与UI协作、代码审查、高可用','本科及以上学历、视觉算法、深度学习、数据库模型设计、微服务项目经验','{"skillMatch": {"score": 82, "suggestions": ["补充系统架构设计、性能优化和复杂问题处理案例", "补充数据库模型设计与查询优化的具体成果", "如有视觉算法或深度学习项目，建议单独说明"]}, "keywordMatch": {"score": 80, "suggestions": ["补充本科及以上学历、计算机相关专业等硬性关键词", "如具备相关经验，补充视觉算法、深度学习关键词", "将微服务架构进一步落到具体项目经验与成果中"]}, "structureMatch": {"score": 45, "suggestions": ["删除[X]、[学校]、[专业]等模板占位符", "完善教育经历、工作经历和项目经历的时间线", "增加项目链接、量化指标和可验证成果"]}, "experienceMatch": {"score": 55, "suggestions": ["补全教育经历，避免空白或模板化内容", "补充真实项目背景、职责与上线成果，减少占位符", "提供高可用、数据库优化、微服务相关的具体实践案例"]}}','候选人技术栈与全栈Java岗位较匹配，覆盖Java、Spring Boot、MySQL、Vue、微服务和前后端协作等核心能力。但简历模板化明显，教育背景、项目细节、量化成果缺失，且视觉算法、深度学习和本科信息等要求未体现。建议补全真实项目、学历信息、数据库设计与高可用成果，以提升可信度和岗位匹配度。',1,'2026-09-02 10:32:59');
INSERT INTO `moyun-db`.portal_resume_job_target (user_id,`position`,company,city,job_type,jd_text,jd_keywords,is_default,create_time,update_time) VALUES
    (6,'java','','深圳市','全职','职位名称：全栈工程师
薪资范围：25-50K·15薪

职位描述摘要：
寻找经验丰富的全栈工程师，负责设计、开发和维护Web应用程序，参与完整开发生命周期，协作确保高质量交付。具备视觉算法经验者优先。

岗位职责：

参与系统架构设计，负责实施可扩展的解决方案；

开发维护高性能、可扩展的Web应用，保障稳定性和安全性；

设计实现数据库模型，优化查询与操作；

与产品经理和UI/UX设计师紧密合作，确保用户体验与功能实现；

参与代码审查，提供技术指导与支持；

解决系统故障，保障高可用性。

岗位要求：

熟悉至少一种后端语言（Go、Python、Java等）及对应框架；

精通HTML、CSS、JavaScript，熟练使用前端框架（如Vue）；

熟悉关系型数据库（如MySQL）的设计与优化；

熟悉微服务架构，有相关项目经验者优先；

良好的沟通与团队协作能力，适应敏捷开发；

自我驱动，具备独立解决复杂问题的能力；

学习能力强，对新技术保持敏锐；

本科及以上学历（计算机、软件工程等相关专业优先），有深度学习相关技术经验者优先。',NULL,0,'2026-09-01 09:04:38','2026-09-01 09:04:38');
INSERT INTO `moyun-db`.portal_resume_optimize_history (user_id,resume_id,from_resume_id,job_target_id,score_before,score_after,match_score_before,match_score_after,adopted_count,total_count,optimize_data,create_time) VALUES
                                                                                                                                                                                                                             (6,1,1,2,11,21,3,NULL,4,4,'{"items": [{"field": "description", "index": 0, "reason": "补全求职意向并对齐全栈岗位。", "status": "adopted", "section": "objective", "original": null, "optimized": "求职意向：目标岗位Java全栈工程师，意向城市[城市]，工作类型[全职/远程]。面向Web应用全生命周期开发，聚焦高可用后端服务与Vue前端实现，愿意参与架构设计、数据库优化与代码评审，保障系统稳定性与用户体验。"}, {"field": "description", "index": 0, "reason": "技能关键词精准匹配JD。", "status": "adopted", "section": "skills", "original": "", "optimized": "精通：Java、Spring Boot、MySQL\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、[视觉算法/深度学习]"}, {"field": "description", "index": 0, "reason": "突出全栈、协作与高可用能力。", "status": "adopted", "section": "selfIntro", "original": "", "optimized": "具备[X]年Java Web开发经验，参与后端服务、数据库设计与前端Vue页面开发。熟悉微服务拆分、接口设计、MySQL查询优化与代码评审，能够与产品、UI/UX协作推进敏捷迭代。面对系统故障，能定位瓶颈并优化可用性至[X%]。"}, {"field": "description", "index": 0, "reason": "用STAR与量化占位补项目。", "status": "adopted", "section": "project", "original": null, "optimized": "[项目名称]：基于Java+Spring Boot+Vue的Web应用。职责：负责[模块]设计、接口开发、MySQL表结构优化与前端联调。行动：采用微服务/缓存/异步处理解决[问题]。结果：响应时间降低[X%]，并发能力提升[X%]，可用性达[X%]。"}], "summary": "围绕Java全栈岗位，优化求职意向、技能、自我介绍与项目模板，使用占位符避免编造。"}','2026-09-01 09:51:07'),
                                                                                                                                                                                                                             (6,1,1,2,21,41,62,NULL,6,6,'{"items": [{"field": "position", "index": 0, "reason": "明确目标岗位，匹配全栈与Java要求。", "status": "adopted", "section": "objective", "original": "", "optimized": "求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。"}, {"field": "description", "index": 0, "reason": "教育信息缺失，需补齐关键信息。", "status": "adopted", "section": "education", "original": "阿萨v撒啊 俺的", "optimized": "学校/专业/学历/时间：[学校] · [专业] · [学历] · [起止时间]；主修课程：[课程]；荣誉奖项：[奖项]；若有深度学习、视觉算法或计算机相关项目，请补充项目名称、职责与成果。"}, {"field": "description", "index": 0, "reason": "突出全栈交付与量化结果。", "status": "adopted", "section": "work", "original": "\\n      负责基于 Java、Spring Boot、MySQL 与 Vue 的功能开发及接口联调，完成需求理解、编码实现、自测与文档沉淀；通过规范接口设计和复用公共组件，支撑 [X] 个模块上线，开发效率提升 [X%]，缺陷率下降 [X%]，累计沉淀技术文档 [X] 篇。\\n     突出 Java、Spring Boot、MySQL、Vue 和微服务相关技术栈，强化工程实现与文档规范性。\\n    基于微服务架构参与前后端功能开发，使用 Spring Boot 实现服务端接口，结合 MySQL 完成数据访问，并通过 Vue、HTML、CSS、JavaScript 构建页面交互；同步输出接口说明、实现方案与变更记录，提升代码可维护性与问题定位效率 [X%]。 \\n \\"强调编码与文档对需求交付、协作效率及业务上线的支撑价值。\\n  围绕业务需求完成功能开发、页面实现与文档沉淀，将需求转化为可交付代码与可复用资料；通过整理接口说明、操作流程和变更记录，降低协作沟通成本 [X%]，支撑 [X] 个需求按期上线，提升业务响应效率 [X%]。\\n", "optimized": "负责基于Java、Spring Boot、MySQL、Vue与微服务架构的Web功能开发，参与需求评审、接口设计、编码实现、联调自测及文档沉淀；配合产品与UI/UX完成前后端集成，优化SQL查询与公共组件，支撑 [X] 个模块上线，开发效率提升 [X%]，缺陷率下降 [X%]，接口问题定位效率提升 [X%]。"}, {"field": "description", "index": 0, "reason": "重构乱码描述，补充真实项目价值。", "status": "adopted", "section": "project", "original": "阿斯顿v啊啊俺的啊", "optimized": "墨韵项目：围绕[业务场景]完成系统设计与功能实现，负责[前端页面/后端接口/数据库设计/联调测试]中的[具体职责]，使用[Java/Spring Boot/MySQL/Vue等真实技术]支撑[功能模块]交付，提升[业务指标/效率] [X%]，降低[问题率/成本] [X%]。"}, {"field": "description", "index": 0, "reason": "分层展示技能，匹配全栈要求。", "status": "adopted", "section": "skills", "original": "Java(精通)、Spring Boot(精通)、MySQL(精通)、Vue(熟练)、HTML(熟练)、CSS(熟练)、JavaScript(熟练)、微服务架构(熟练)", "optimized": "精通：Java、Spring Boot、MySQL、SQL查询优化、接口设计\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、前后端联调、代码文档沉淀"}, {"field": "description", "index": 0, "reason": "强化全栈、协作与稳定性保障。", "status": "adopted", "section": "selfIntro", "original": "具备[X]年Java Web开发经验，参与后端服务、数据库设计与前端Vue页面开发。熟悉微服务拆分、接口设计、MySQL查询优化与代码评审，能够与产品、UI/UX协作推进敏捷迭代。面对系统故障，能定位瓶颈并优化可用性至[X%]。", "optimized": "具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript及微服务架构。能够参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；面对系统故障可定位瓶颈并优化可用性至[X%]。"}], "summary": "围绕全栈岗位补齐意向、教育、工作、项目与技能，突出Java、Vue、MySQL与微服务。"}','2026-09-02 10:29:31'),
                                                                                                                                                                                                                             (6,1,1,2,41,41,66,NULL,6,6,'{"items": [{"field": "description", "index": 0, "reason": "明确全栈定位，覆盖JD核心关键词。", "status": "adopted", "section": "objective", "original": null, "optimized": "求职目标为全栈工程师/Java后端开发工程师，聚焦Spring Boot、Vue、MySQL、微服务架构与Web应用全生命周期开发，重点突出接口设计、前后端联调、SQL优化、高可用系统建设，并与产品、UI/UX协作推动高质量交付。"}, {"field": "description", "index": 0, "reason": "补强学历与算法加分项。", "status": "adopted", "section": "education", "original": "学校/专业/学历/时间：[学校] · [专业] · [学历] · [起止时间]；主修课程：[课程]；荣誉奖项：[奖项]；若有深度学习、视觉算法或计算机相关项目，请补充项目名称、职责与成果。", "optimized": "[学校] · [专业] · [学历] · [起止时间]；主修[课程]，获得[奖项/荣誉]；参与[深度学习/视觉算法/计算机项目名称]，负责[职责]，使用[技术栈]完成[成果]，量化体现[指标提升X%]。"}, {"field": "description", "index": 0, "reason": "对齐职责要求，突出全栈与量化结果。", "status": "adopted", "section": "work", "original": "负责基于Java、Spring Boot、MySQL、Vue与微服务架构的Web功能开发，参与需求评审、接口设计、编码实现、联调自测及文档沉淀；配合产品与UI/UX完成前后端集成，优化SQL查询与公共组件，支撑 [X] 个模块上线，开发效率提升 [X%]，缺陷率下降 [X%]，接口问题定位效率提升 [X%]。", "optimized": "负责[业务场景]Web应用的全栈开发与交付，基于Java、Spring Boot、MySQL、Vue及微服务架构，参与需求评审、接口设计、数据库优化、前后端联调与文档沉淀；配合产品与UI/UX完成[X]个模块上线，开发效率提升[X%]，缺陷率下降[X%]，接口问题定位效率提升[X%]。"}, {"field": "description", "index": 0, "reason": "体现总师职责与端到端交付能力。", "status": "adopted", "section": "project", "original": "墨韵项目：围绕[业务场景]完成系统设计与功能实现，负责[前端页面/后端接口/数据库设计/联调测试]中的[具体职责]，使用[Java/Spring Boot/MySQL/Vue等真实技术]支撑[功能模块]交付，提升[业务指标/效率] [X%]，降低[问题率/成本] [X%]。", "optimized": "作为墨韵项目总设计师，围绕[业务场景]完成系统方案、模块拆分、数据库模型与接口规范设计；基于Java、Spring Boot、MySQL、Vue组织前后端开发、联调测试与上线验收，支撑[功能模块]交付，提升[业务指标][X%]，降低[问题率/成本][X%]。"}, {"field": "description", "index": 0, "reason": "分层展示技能，匹配全栈技术栈。", "status": "adopted", "section": "skills", "original": "Java(精通)、Spring Boot(精通)、MySQL(精通)、Vue(熟练)、HTML(熟练)、CSS(熟练)、JavaScript(熟练)、微服务架构(熟练)、SQL查询优化(精通)、接口设计(精通)、前后端联调(熟练)、代码文档沉淀(熟练)", "optimized": "精通：Java、Spring Boot、MySQL、SQL查询优化、REST接口设计、数据库模型设计\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、前后端联调、代码审查、技术文档沉淀"}, {"field": "description", "index": 0, "reason": "突出全栈、高可用与协作能力。", "status": "adopted", "section": "selfIntro", "original": "具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript及微服务架构。能够参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；面对系统故障可定位瓶颈并优化可用性至[X%]。", "optimized": "具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript与微服务架构；能参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；擅长定位系统故障并优化可用性至[X%]。"}], "summary": "对齐全栈岗位，强化Java、Vue、MySQL、微服务与量化成果。"}','2026-09-02 11:30:52'),
                                                                                                                                                                                                                             (6,1,1,2,41,41,66,NULL,6,6,'{"items": [{"field": "description", "index": 0, "reason": "明确全栈定位，覆盖JD核心关键词。", "status": "adopted", "section": "objective", "original": null, "optimized": "求职目标为全栈工程师/Java后端开发工程师，聚焦Spring Boot、Vue、MySQL、微服务架构与Web应用全生命周期开发，重点突出接口设计、前后端联调、SQL优化、高可用系统建设，并与产品、UI/UX协作推动高质量交付。"}, {"field": "description", "index": 0, "reason": "补强学历与算法加分项。", "status": "adopted", "section": "education", "original": "学校/专业/学历/时间：[学校] · [专业] · [学历] · [起止时间]；主修课程：[课程]；荣誉奖项：[奖项]；若有深度学习、视觉算法或计算机相关项目，请补充项目名称、职责与成果。", "optimized": "[学校] · [专业] · [学历] · [起止时间]；主修[课程]，获得[奖项/荣誉]；参与[深度学习/视觉算法/计算机项目名称]，负责[职责]，使用[技术栈]完成[成果]，量化体现[指标提升X%]。"}, {"field": "description", "index": 0, "reason": "对齐职责要求，突出全栈与量化结果。", "status": "adopted", "section": "work", "original": "负责基于Java、Spring Boot、MySQL、Vue与微服务架构的Web功能开发，参与需求评审、接口设计、编码实现、联调自测及文档沉淀；配合产品与UI/UX完成前后端集成，优化SQL查询与公共组件，支撑 [X] 个模块上线，开发效率提升 [X%]，缺陷率下降 [X%]，接口问题定位效率提升 [X%]。", "optimized": "负责[业务场景]Web应用的全栈开发与交付，基于Java、Spring Boot、MySQL、Vue及微服务架构，参与需求评审、接口设计、数据库优化、前后端联调与文档沉淀；配合产品与UI/UX完成[X]个模块上线，开发效率提升[X%]，缺陷率下降[X%]，接口问题定位效率提升[X%]。"}, {"field": "description", "index": 0, "reason": "体现总师职责与端到端交付能力。", "status": "adopted", "section": "project", "original": "墨韵项目：围绕[业务场景]完成系统设计与功能实现，负责[前端页面/后端接口/数据库设计/联调测试]中的[具体职责]，使用[Java/Spring Boot/MySQL/Vue等真实技术]支撑[功能模块]交付，提升[业务指标/效率] [X%]，降低[问题率/成本] [X%]。", "optimized": "作为墨韵项目总设计师，围绕[业务场景]完成系统方案、模块拆分、数据库模型与接口规范设计；基于Java、Spring Boot、MySQL、Vue组织前后端开发、联调测试与上线验收，支撑[功能模块]交付，提升[业务指标][X%]，降低[问题率/成本][X%]。"}, {"field": "description", "index": 0, "reason": "分层展示技能，匹配全栈技术栈。", "status": "adopted", "section": "skills", "original": "Java(精通)、Spring Boot(精通)、MySQL(精通)、Vue(熟练)、HTML(熟练)、CSS(熟练)、JavaScript(熟练)、微服务架构(熟练)、SQL查询优化(精通)、接口设计(精通)、前后端联调(熟练)、代码文档沉淀(熟练)", "optimized": "精通：Java、Spring Boot、MySQL、SQL查询优化、REST接口设计、数据库模型设计\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、前后端联调、代码审查、技术文档沉淀"}, {"field": "description", "index": 0, "reason": "突出全栈、高可用与协作能力。", "status": "adopted", "section": "selfIntro", "original": "具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript及微服务架构。能够参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；面对系统故障可定位瓶颈并优化可用性至[X%]。", "optimized": "具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript与微服务架构；能参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；擅长定位系统故障并优化可用性至[X%]。"}], "summary": "对齐全栈岗位，强化Java、Vue、MySQL、微服务与量化成果。"}','2026-09-02 11:30:54');
INSERT INTO `moyun-db`.portal_resume_optimize_task (user_id,resume_id,job_target_id,status,result_json,error_msg,ai_powered,create_time,start_time,finish_time) VALUES
                                                                                                                                                                    (6,1,2,'success','{"items": [{"field": "description", "index": 0, "reason": "补全求职意向并对齐全栈岗位。", "section": "objective", "original": null, "optimized": "求职意向：目标岗位Java全栈工程师，意向城市[城市]，工作类型[全职/远程]。面向Web应用全生命周期开发，聚焦高可用后端服务与Vue前端实现，愿意参与架构设计、数据库优化与代码评审，保障系统稳定性与用户体验。"}, {"field": "description", "index": 0, "reason": "技能关键词精准匹配JD。", "section": "skills", "original": "", "optimized": "精通：Java、Spring Boot、MySQL\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、[视觉算法/深度学习]"}, {"field": "description", "index": 0, "reason": "突出全栈、协作与高可用能力。", "section": "selfIntro", "original": "", "optimized": "具备[X]年Java Web开发经验，参与后端服务、数据库设计与前端Vue页面开发。熟悉微服务拆分、接口设计、MySQL查询优化与代码评审，能够与产品、UI/UX协作推进敏捷迭代。面对系统故障，能定位瓶颈并优化可用性至[X%]。"}, {"field": "description", "index": 0, "reason": "用STAR与量化占位补项目。", "section": "project", "original": null, "optimized": "[项目名称]：基于Java+Spring Boot+Vue的Web应用。职责：负责[模块]设计、接口开发、MySQL表结构优化与前端联调。行动：采用微服务/缓存/异步处理解决[问题]。结果：响应时间降低[X%]，并发能力提升[X%]，可用性达[X%]。"}], "summary": "围绕Java全栈岗位，优化求职意向、技能、自我介绍与项目模板，使用占位符避免编造。", "resumeId": "1", "aiPowered": true, "jobTargetId": "2"}',NULL,1,'2026-09-01 09:49:13','2026-09-01 09:49:13','2026-09-01 09:50:25'),
                                                                                                                                                                    (6,1,2,'success','{"items": [{"field": "description", "index": 0, "reason": "明确岗位与城市，匹配全栈招聘目标。", "section": "objective", "original": null, "optimized": "求职意向：全栈工程师/Java开发工程师；目标城市：[城市]；工作类型：全职；期望薪资：25-50K·15薪。围绕Web应用全生命周期开发，突出Java后端、MySQL优化、Vue前端协作与微服务实践，匹配高可用系统交付目标。"}, {"field": "description", "index": 0, "reason": "对齐JD技术栈，体现前后端与微服务能力。", "section": "skills", "original": "Java(精通)、Spring Boot(精通)、MySQL(精通)、Vue(熟练)、HTML(熟练)、CSS(熟练)、JavaScript(熟练)、微服务架构(熟练)", "optimized": "精通：Java、Spring Boot、MySQL、微服务架构、RESTful接口设计、SQL查询优化、数据库模型设计、高可用Web服务开发。\\n熟练：Vue、HTML、CSS、JavaScript、敏捷协作、代码审查、Git、Maven、故障定位与性能优化。"}, {"field": "description", "index": 0, "reason": "以量化占位符呈现成果，突出高可用能力。", "section": "selfIntro", "original": "具备[X]年Java Web开发经验，参与后端服务、数据库设计与前端Vue页面开发。熟悉微服务拆分、接口设计、MySQL查询优化与代码评审，能够与产品、UI/UX协作推进敏捷迭代。面对系统故障，能定位瓶颈并优化可用性至[X%]。", "optimized": "具备[X]年Java Web开发经验，参与需求分析、架构设计、后端接口开发、MySQL建模优化及Vue前端实现。曾支撑微服务模块上线，通过SQL优化与接口治理提升查询效率[X%]，保障可用性达[X%]。能与产品、UI/UX敏捷协作，主动定位系统故障并推动复盘。"}], "summary": "围绕全栈Java岗位强化意向、技能与自评，突出微服务、Vue与量化成果。", "resumeId": "1", "aiPowered": true, "jobTargetId": "2"}',NULL,1,'2026-09-01 10:41:45','2026-09-01 10:41:45','2026-09-01 10:43:03'),
                                                                                                                                                                    (6,1,2,'success','{"items": [{"field": "position", "index": 0, "reason": "对齐全栈岗位，补齐关键求职意向。", "section": "objective", "original": "", "optimized": "求职意向：全栈工程师/Java开发工程师，目标城市：[目标城市]，期望类型：全职。聚焦Java、Spring Boot、MySQL、Vue与微服务架构，参与Web应用设计、开发、维护及高质量交付。"}, {"field": "description", "index": 0, "reason": "分层展示技能，匹配JD要求。", "section": "skills", "original": "Java(精通)、Spring Boot(精通)、MySQL(精通)、Vue(熟练)、HTML(熟练)、CSS(熟练)、JavaScript(熟练)、微服务架构(熟练)", "optimized": "精通：Java、Spring Boot、MySQL、接口设计、SQL查询优化、系统稳定性保障\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、代码评审、敏捷协作"}, {"field": "description", "index": 0, "reason": "突出全栈交付与可用性量化。", "section": "selfIntro", "original": "具备[X]年Java Web开发经验，参与后端服务、数据库设计与前端Vue页面开发。熟悉微服务拆分、接口设计、MySQL查询优化与代码评审，能够与产品、UI/UX协作推进敏捷迭代。面对系统故障，能定位瓶颈并优化可用性至[X%]。", "optimized": "具备[X]年Java Web开发经验，参与后端服务、数据库模型设计与前端Vue页面开发。基于Spring Boot与MySQL完成接口开发、查询优化和微服务拆分，通过代码评审与故障定位提升系统稳定性，推动可用性优化至[X%]。能与产品、UI/UX敏捷协作，保障高质量交付。"}], "summary": "对齐全栈岗位，补齐意向并强化技能与稳定性量化。", "resumeId": "1", "aiPowered": true, "jobTargetId": "2"}',NULL,1,'2026-09-01 10:52:08','2026-09-01 10:52:08','2026-09-01 10:53:37'),
                                                                                                                                                                    (6,1,2,'success','{"items": [{"field": "position", "index": 0, "reason": "明确岗位匹配方向，补齐意向缺失。", "section": "objective", "original": "", "optimized": "全栈工程师（Java方向）；目标城市：[城市]；全职。期望负责Java后端、Vue前端与MySQL优化，参与微服务架构设计、代码评审及高可用Web应用交付，与产品、UI/UX协同推进敏捷迭代。"}, {"field": "description", "index": 0, "reason": "分层呈现技能，贴合全栈要求。", "section": "skills", "original": "Java(精通)、Spring Boot(精通)、MySQL(精通)、Vue(熟练)、HTML(熟练)、CSS(熟练)、JavaScript(熟练)、微服务架构(熟练)", "optimized": "精通：Java、Spring Boot、MySQL、数据库设计与查询优化\\\\n熟练：Vue、HTML、CSS、JavaScript、微服务架构、敏捷开发协作"}, {"field": "description", "index": 0, "reason": "突出全栈与故障优化能力。", "section": "selfIntro", "original": "具备[X]年Java Web开发经验，参与后端服务、数据库设计与前端Vue页面开发。熟悉微服务拆分、接口设计、MySQL查询优化与代码评审，能够与产品、UI/UX协作推进敏捷迭代。面对系统故障，能定位瓶颈并优化可用性至[X%]。", "optimized": "具备[X]年Java Web开发经验，参与后端服务、数据库设计与前端Vue页面开发。熟悉微服务拆分、接口设计、MySQL查询优化与代码评审，能够与产品、UI/UX协作推进敏捷迭代。面对系统故障，能定位瓶颈并优化可用性至[X%]，保障Web应用稳定、安全、高性能交付。"}], "summary": "围绕全栈岗位补全意向、技能分层与自我评价，突出Java、Vue与量化占位。", "resumeId": "1", "aiPowered": true, "jobTargetId": "2"}',NULL,1,'2026-09-01 14:34:03','2026-09-01 14:34:03','2026-09-01 14:35:15');
INSERT INTO `moyun-db`.portal_resume_score_report (user_id,resume_id,job_target_id,position_snapshot,score,score_detail,source,create_time) VALUES
                                                                                                                                                (6,1,NULL,NULL,21,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 0,
  "message" : "期望职位缺失；期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 0,
  "message" : "未填写教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 0,
  "message" : "未填写工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 0,
  "message" : "未填写项目经历",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 8 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 114 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "未设置目标岗位，无法评估匹配度（建议在档案页选择目标岗位）",
  "subItems" : null
} ]','manual','2026-09-01 09:51:07'),
                                                                                                                                                (6,1,2,'java',21,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 0,
  "message" : "期望职位缺失；期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 0,
  "message" : "未填写教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 0,
  "message" : "未填写工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 0,
  "message" : "未填写项目经历",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 8 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 114 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "未设置目标岗位，无法评估匹配度（建议在档案页选择目标岗位）",
  "subItems" : null
} ]','optimize','2026-09-01 09:51:20'),
                                                                                                                                                (6,1,NULL,'求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。',41,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 4,
  "message" : "期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 1,
  "message" : "共 1 条教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 8,
  "message" : "共 1 条工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 7,
  "message" : "共 1 个项目",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 12 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 135 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "目标岗位「求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。」未在岗位字典中找到匹配，无法评估匹配度",
  "subItems" : null
} ]','manual','2026-09-02 10:29:31'),
                                                                                                                                                (6,1,2,'java',41,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 4,
  "message" : "期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 1,
  "message" : "共 1 条教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 8,
  "message" : "共 1 条工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 7,
  "message" : "共 1 个项目",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 12 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 135 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "目标岗位「求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。」未在岗位字典中找到匹配，无法评估匹配度",
  "subItems" : null
} ]','optimize','2026-09-02 10:29:40'),
                                                                                                                                                (6,1,2,'java',41,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 4,
  "message" : "期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 1,
  "message" : "共 1 条教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 8,
  "message" : "共 1 条工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 7,
  "message" : "共 1 个项目",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 12 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 135 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "目标岗位「求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。」未在岗位字典中找到匹配，无法评估匹配度",
  "subItems" : null
} ]','optimize','2026-09-02 10:30:40'),
                                                                                                                                                (6,1,2,'java',41,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 4,
  "message" : "期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 1,
  "message" : "共 1 条教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 8,
  "message" : "共 1 条工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 7,
  "message" : "共 1 个项目",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 12 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 135 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "目标岗位「求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。」未在岗位字典中找到匹配，无法评估匹配度",
  "subItems" : null
} ]','optimize','2026-09-02 10:31:25'),
                                                                                                                                                (6,1,NULL,'求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。',41,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 4,
  "message" : "期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 1,
  "message" : "共 1 条教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 8,
  "message" : "共 1 条工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 7,
  "message" : "共 1 个项目",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 12 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 135 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "目标岗位「求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。」未在岗位字典中找到匹配，无法评估匹配度",
  "subItems" : null
} ]','manual','2026-09-02 11:30:32'),
                                                                                                                                                (6,1,NULL,'求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。',41,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 4,
  "message" : "期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 1,
  "message" : "共 1 条教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 8,
  "message" : "共 1 条工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 7,
  "message" : "共 1 个项目",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 16 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 131 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "目标岗位「求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。」未在岗位字典中找到匹配，无法评估匹配度",
  "subItems" : null
} ]','manual','2026-09-02 11:30:52'),
                                                                                                                                                (6,1,NULL,'求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。',41,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 4,
  "message" : "期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 1,
  "message" : "共 1 条教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 8,
  "message" : "共 1 条工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 7,
  "message" : "共 1 个项目",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 16 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 131 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "目标岗位「求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。」未在岗位字典中找到匹配，无法评估匹配度",
  "subItems" : null
} ]','manual','2026-09-02 11:30:54');
INSERT INTO `moyun-db`.portal_study_plan (user_id,title,plan_type,target_count,target_category,start_date,end_date,status,created_time,del_flag) VALUES
    (6,'每日装逼10道题','daily_question',10,'java','2026-08-31','2026-09-30','active','2026-08-31 13:47:58','0');
INSERT INTO `moyun-db`.portal_study_plan_log (plan_id,user_id,log_date,done_count,created_time) VALUES
    (1,6,'2026-08-31',29,'2026-08-31 13:48:09');
INSERT INTO `moyun-db`.portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                 ('生活哲思','life-philosophy',1,'0',NULL,3,'admin','2026-08-19 18:01:44','','2026-09-07 10:15:28','人文类','0'),
                                                                                                                                                 ('城市记忆','city-memory',2,'0',NULL,3,'admin','2026-08-19 18:01:44','','2026-08-31 11:25:15','人文类','0'),
                                                                                                                                                 ('自然写作','nature-writing',3,'0',NULL,4,'admin','2026-08-19 18:01:44','','2026-09-07 10:15:28','人文类','0'),
                                                                                                                                                 ('情感随笔','emotional-essay',4,'0',NULL,2,'admin','2026-08-19 18:01:44','','2026-08-28 11:22:25','人文类','0'),
                                                                                                                                                 ('人间烟火','life-fireworks',5,'0',NULL,2,'admin','2026-08-19 18:01:44','','2026-08-31 11:25:15','人文类','0'),
                                                                                                                                                 ('乡愁记忆','nostalgia',6,'0',NULL,3,'admin','2026-08-19 18:01:44','','2026-09-07 10:15:28','人文类','0'),
                                                                                                                                                 ('孤独成长','loneliness-growth',7,'0',NULL,5,'admin','2026-08-19 18:01:44','','2026-09-07 10:03:06','人文类','0'),
                                                                                                                                                 ('四季感悟','seasons-feeling',8,'0',NULL,2,'admin','2026-08-19 18:01:44','','2026-09-07 10:15:28','人文类','0'),
                                                                                                                                                 ('SpringBoot实战','springboot-practice',9,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
                                                                                                                                                 ('React Hooks','react-hooks',10,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0');
INSERT INTO `moyun-db`.portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                 ('AI辅助开发','ai-assisted-dev',11,'0',NULL,1,'admin','2026-08-19 18:01:44','','2026-09-07 10:03:06','技术类','0'),
                                                                                                                                                 ('算法突破','algorithm-breakthrough',12,'0',NULL,1,'admin','2026-08-19 18:01:44','','2026-09-07 10:03:06','技术类','0'),
                                                                                                                                                 ('Java并发','java-concurrency',13,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
                                                                                                                                                 ('Vue3实践','vue3-practice',14,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
                                                                                                                                                 ('微服务架构','microservices',15,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
                                                                                                                                                 ('MySQL优化','mysql-optimization',16,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
                                                                                                                                                 ('Git协作','git-collaboration',17,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
                                                                                                                                                 ('前端性能','frontend-performance',18,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
                                                                                                                                                 ('JVM调优','jvm-tuning',19,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
                                                                                                                                                 ('系统设计','system-design',20,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0');
INSERT INTO `moyun-db`.portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                 ('新手入门','beginner-guide',21,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
                                                                                                                                                 ('进阶提升','advanced-improvement',22,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
                                                                                                                                                 ('面试备战','interview-prep',23,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
                                                                                                                                                 ('读书心得','reading-notes',24,'0',NULL,1,'admin','2026-08-19 18:01:44','','2026-08-28 11:13:21','通用类','0'),
                                                                                                                                                 ('写作技巧','writing-tips',25,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
                                                                                                                                                 ('学习方法','learning-methods-tag',26,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
                                                                                                                                                 ('职场经验','career-experience',27,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
                                                                                                                                                 ('个人成长','personal-growth',28,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
                                                                                                                                                 ('java springboot',NULL,0,'0','interview_experience',2,'','2026-08-31 10:42:07','','2026-09-07 10:03:06',NULL,'0');
INSERT INTO `moyun-db`.portal_task (code,name,description,task_type,reward_points,target_count,icon,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                            ('daily_checkin','每日签到','每天签到一次，保持活跃','daily',10,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),
                                                                                                                                                                            ('daily_publish','每日发文','每日发布 1 篇文章','daily',20,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),
                                                                                                                                                                            ('daily_comment','每日互动','每日评论 3 次','daily',15,3,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),
                                                                                                                                                                            ('daily_like','每日点赞','每日点赞 5 次','daily',10,5,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),
                                                                                                                                                                            ('daily_solve','每日刷题','每日解答 3 道面试题','daily',20,3,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),
                                                                                                                                                                            ('first_article','初露锋芒','发布第一篇文章','achievement',50,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),
                                                                                                                                                                            ('solve_50','刷题能手','累计解答 50 道面试题','achievement',200,50,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
INSERT INTO `moyun-db`.portal_tip_order (user_id,author_id,target_type,target_id,amount,message,status,pay_method,trade_no,pay_channel,notify_id,notify_time,refund_no,refund_amount,refund_time,refund_reason,paid_time,created_time) VALUES
                                                                                                                                                                                                                                           (7,6,'article',1,10.00,'','pending','wechat',NULL,'points',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 10:04:44'),
                                                                                                                                                                                                                                           (7,6,'article',1,10.00,'','pending','wechat',NULL,'points',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 10:20:16'),
                                                                                                                                                                                                                                           (7,6,'article',1,5.00,'','pending','wechat',NULL,'points',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 10:21:03'),
                                                                                                                                                                                                                                           (7,6,'article',1,5.00,'','pending','wechat',NULL,'points',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 10:21:15'),
                                                                                                                                                                                                                                           (7,6,'article',1,5.00,'','pending','wechat',NULL,'points',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 10:21:36'),
                                                                                                                                                                                                                                           (7,6,'article',1,5.00,'','paid','wechat',NULL,'points',NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-28 10:27:19','2026-08-28 10:26:19');
INSERT INTO `moyun-db`.portal_topic (title,description,cover,creator_id,status,auditor_id,audit_remark,audit_time,pinned,view_count,post_count,like_count,is_featured,comment_count,last_post_time,last_poster_id,created_time,updated_time,del_flag) VALUES
    ('哈哈哈，世界太变态了？','世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？','http://127.0.0.1:9001/moyun/2026/09/07/59caaf2c79f94d9c804ddb6618cbfe7e.jpg',6,'active',1,NULL,'2026-09-07 17:52:45',0,2,2,0,0,2,'2026-09-07 17:53:08',6,'2026-09-07 17:52:17','2026-09-07 17:53:16','0');
INSERT INTO `moyun-db`.portal_topic_comment (target_type,target_id,author_id,content,parent_id,root_id,reply_to,reply_to_content,like_count,reply_count,is_deleted,created_time,updated_time) VALUES
                                                                                                                                                                                                  ('topic',1,6,'24233223',0,0,NULL,'',0,0,0,'2026-09-07 17:53:13',NULL),
                                                                                                                                                                                                  ('topic',1,6,'QEFQWWECRWCWEC',0,0,NULL,'',0,0,0,'2026-09-07 17:53:16',NULL);
INSERT INTO `moyun-db`.portal_topic_post (topic_id,user_id,content,images,parent_post_id,reply_to_user_id,floor,like_count,comment_count,is_deleted,created_time,updated_time) VALUES
                                                                                                                                                                                   (1,6,'SC A撒A DA',NULL,NULL,NULL,1,0,0,0,'2026-09-07 17:53:02',NULL),
                                                                                                                                                                                   (1,6,'ADQQQQQQ213421444444444444444444444444444',NULL,NULL,NULL,2,0,0,0,'2026-09-07 17:53:08',NULL);
INSERT INTO `moyun-db`.portal_user (user_id,username,nickname,email,phone,password,avatar,bio,`position`,identity_tag,wechat,gender,birthday,location,website,github,company,school,`language`,timezone,notify_like,notify_comment,notify_follow,notify_system,privacy_follow,privacy_bookmark,privacy_email,privacy_phone,privacy_profile,`role`,is_certified_creator,vip_expire_at,is_phone_verified,is_wechat_verified,two_factor_enabled,status,del_flag,login_ip,login_date,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         (NULL,'zhangsan','','19987671567@163.com','','$2a$10$iOnd69MDurSwpUGCSYsIn.sn8Ki1S3xLFBWntjZCXmJSEbFsBhEFK','http://127.0.0.1:9001/moyun/2026/09/02/a65c86424ed64626a710b4ecdc638a0c.jpg','','工程师','office_worker','','','','','','','慧博云通','',NULL,NULL,1,1,1,1,1,1,0,0,1,'user',0,NULL,0,0,0,'0','0','127.0.0.1','2026-09-11 16:23:28','','2026-08-20 08:56:07','','2026-09-11 16:23:28',NULL),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         (NULL,'libai','李白','13164798225@163.com','13164798225','$2a$10$gvpTjHffDLB1nRO6AoJ8g.ysZXm8tvn2ZaUOkJZkn7ec1M.pNZJj2','http://127.0.0.1:9001/moyun/2026/08/28/0d0edb42422c464cbfd548fcbe470efb.jpg','11111111111',NULL,NULL,NULL,'male',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,1,'user',1,NULL,0,0,0,'0','0','127.0.0.1','2026-09-04 12:50:56','','2026-08-28 10:01:26','','2026-09-04 12:50:55',NULL),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         (1,'admin','若依','ry@163.com','15888888888',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,1,'admin',0,NULL,0,0,0,'0','0','',NULL,'','2026-08-28 11:22:25','','2026-08-28 11:22:25',NULL),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         (NULL,'lisi',NULL,NULL,'18218361923','$2a$10$Dwc04XACTC0JjfIT1ljyCuYySRq0Sd4.f7b0UaPg4hrp89hizxgUi',NULL,NULL,'程序员','office_worker',NULL,NULL,NULL,NULL,NULL,NULL,'慧博云通',NULL,NULL,NULL,1,1,1,1,1,1,0,0,1,'user',0,NULL,0,0,0,'0','0','127.0.0.1','2026-09-09 15:31:20','','2026-09-09 14:54:37','','2026-09-09 15:31:20',NULL),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         (NULL,'moyun_official','墨云官方',NULL,NULL,NULL,NULL,'墨云官方账号 · 每日话题由 AI 生成',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,1,0,0,0,0,'admin',1,NULL,0,0,0,'0','0','',NULL,'admin','2026-09-11 14:53:02','','2026-09-11 14:53:02','系统账号：AI 生成话题专用，SQL 20260911-04 初始化');
INSERT INTO `moyun-db`.portal_user_badge (user_id,achievement_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                         (6,1,'','2026-08-28 09:58:10','','2026-08-28 09:58:10',NULL),
                                                                                                                         (7,1,'','2026-08-28 13:12:22','','2026-08-28 13:12:22',NULL),
                                                                                                                         (7,11,'','2026-08-28 13:44:02','','2026-08-28 13:44:02',NULL),
                                                                                                                         (6,24,'','2026-08-31 10:14:30','','2026-08-31 10:14:30',NULL),
                                                                                                                         (6,16,'','2026-08-31 10:42:06','','2026-08-31 10:42:06',NULL),
                                                                                                                         (6,11,'','2026-09-07 17:08:33','','2026-09-07 17:08:33',NULL);
INSERT INTO `moyun-db`.portal_user_growth (user_id,growth_value,`level`,title,season_value,points,supplement_card_count,last_card_grant_month,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                                                      (6,397,3,'锋芒初露',397,0,0,NULL,'','2026-08-28 09:58:10','','2026-09-07 17:53:08',NULL),
                                                                                                                                                                                                      (7,171,2,'小试牛刀',171,0,0,NULL,'','2026-08-28 13:12:22','','2026-09-02 14:22:39',NULL);
INSERT INTO `moyun-db`.portal_user_resume (user_id,title,parent_id,version_no,name,gender,birth_date,phone,email,avatar,job_intention,educations,works,projects,skills,self_intro,full_text,score,score_detail,parse_confidence,scored_time,file_url,export_time,status,source_type,source_file_url,source_file_name,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                                                                                      (6,'zhangsan的简历',NULL,1,'zhangsan','',NULL,'','19987671567@163.com','','{
  "position" : "求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。",
  "city" : "",
  "salaryMin" : null,
  "salaryMax" : null,
  "jobType" : "",
  "availableTime" : ""
}','[ {
  "school" : "",
  "major" : "",
  "degree" : "",
  "startDate" : "2026-06",
  "endDate" : "2026-07",
  "description" : "[学校] · [专业] · [学历] · [起止时间]；主修[课程]，获得[奖项/荣誉]；参与[深度学习/视觉算法/计算机项目名称]，负责[职责]，使用[技术栈]完成[成果]，量化体现[指标提升X%]。"
} ]','[ {
  "company" : "宇信科技",
  "position" : "高级Java工程师",
  "startDate" : "2026-09",
  "endDate" : "2026-12",
  "description" : "负责[业务场景]Web应用的全栈开发与交付，基于Java、Spring Boot、MySQL、Vue及微服务架构，参与需求评审、接口设计、数据库优化、前后端联调与文档沉淀；配合产品与UI/UX完成[X]个模块上线，开发效率提升[X%]，缺陷率下降[X%]，接口问题定位效率提升[X%]。"
} ]','[ {
  "name" : "墨韵项目",
  "role" : "总设计师",
  "startDate" : "2026-06",
  "endDate" : "2026-12",
  "description" : "作为墨韵项目总设计师，围绕[业务场景]完成系统方案、模块拆分、数据库模型与接口规范设计；基于Java、Spring Boot、MySQL、Vue组织前后端开发、联调测试与上线验收，支撑[功能模块]交付，提升[业务指标][X%]，降低[问题率/成本][X%]。",
  "url" : ""
} ]','[ {
  "name" : "Java",
  "level" : "精通",
  "category" : null
}, {
  "name" : "Spring Boot",
  "level" : "精通",
  "category" : null
}, {
  "name" : "MySQL",
  "level" : "精通",
  "category" : null
}, {
  "name" : "Vue",
  "level" : "熟练",
  "category" : null
}, {
  "name" : "HTML",
  "level" : "熟练",
  "category" : null
}, {
  "name" : "CSS",
  "level" : "熟练",
  "category" : null
}, {
  "name" : "JavaScript",
  "level" : "熟练",
  "category" : null
}, {
  "name" : "微服务架构",
  "level" : "熟练",
  "category" : null
}, {
  "name" : "SQL查询优化",
  "level" : "精通",
  "category" : null
}, {
  "name" : "接口设计",
  "level" : "精通",
  "category" : null
}, {
  "name" : "前后端联调",
  "level" : "熟练",
  "category" : null
}, {
  "name" : "代码文档沉淀",
  "level" : "熟练",
  "category" : null
}, {
  "name" : "REST接口设计",
  "level" : "精通",
  "category" : null
}, {
  "name" : "数据库模型设计",
  "level" : "精通",
  "category" : null
}, {
  "name" : "代码审查",
  "level" : "熟练",
  "category" : null
}, {
  "name" : "技术文档沉淀",
  "level" : "熟练",
  "category" : null
} ]','具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript与微服务架构；能参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；擅长定位系统故障并优化可用性至[X%]。','姓名：zhangsan
性别：
电话：
邮箱：19987671567@163.com

【求职意向】
期望职位：求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。
期望城市：
工作性质：

【教育经历】
1.  · （）  2026-06 - 2026-07
   [学校] · [专业] · [学历] · [起止时间]；主修[课程]，获得[奖项/荣誉]；参与[深度学习/视觉算法/计算机项目名称]，负责[职责]，使用[技术栈]完成[成果]，量化体现[指标提升X%]。

【工作经历】
1. 宇信科技 · 高级Java工程师  2026-09 - 2026-12
   负责[业务场景]Web应用的全栈开发与交付，基于Java、Spring Boot、MySQL、Vue及微服务架构，参与需求评审、接口设计、数据库优化、前后端联调与文档沉淀；配合产品与UI/UX完成[X]个模块上线，开发效率提升[X%]，缺陷率下降[X%]，接口问题定位效率提升[X%]。

【项目经历】
1. 墨韵项目（总设计师）  2026-06 - 2026-12
   作为墨韵项目总设计师，围绕[业务场景]完成系统方案、模块拆分、数据库模型与接口规范设计；基于Java、Spring Boot、MySQL、Vue组织前后端开发、联调测试与上线验收，支撑[功能模块]交付，提升[业务指标][X%]，降低[问题率/成本][X%]。
   链接：

【专业技能】
- Java（精通）
- Spring Boot（精通）
- MySQL（精通）
- Vue（熟练）
- HTML（熟练）
- CSS（熟练）
- JavaScript（熟练）
- 微服务架构（熟练）
- SQL查询优化（精通）
- 接口设计（精通）
- 前后端联调（熟练）
- 代码文档沉淀（熟练）
- REST接口设计（精通）
- 数据库模型设计（精通）
- 代码审查（熟练）
- 技术文档沉淀（熟练）

【自我评价】
具备[X]年Java Web全栈开发经验，熟悉Spring Boot、MySQL、Vue、HTML/CSS/JavaScript与微服务架构；能参与架构设计、数据库优化、接口评审、代码审查与敏捷协作，推动前后端高质量交付；擅长定位系统故障并优化可用性至[X%]。',41,'[ {
  "item" : "基本信息",
  "maxScore" : 20,
  "score" : 11,
  "message" : "电话缺失；",
  "subItems" : null
}, {
  "item" : "求职意向",
  "maxScore" : 10,
  "score" : 4,
  "message" : "期望城市缺失；期望薪资缺失；",
  "subItems" : null
}, {
  "item" : "教育经历",
  "maxScore" : 15,
  "score" : 1,
  "message" : "共 1 条教育经历",
  "subItems" : null
}, {
  "item" : "工作经历",
  "maxScore" : 25,
  "score" : 8,
  "message" : "共 1 条工作经历",
  "subItems" : null
}, {
  "item" : "项目经历",
  "maxScore" : 20,
  "score" : 7,
  "message" : "共 1 个项目",
  "subItems" : null
}, {
  "item" : "技能列表",
  "maxScore" : 5,
  "score" : 5,
  "message" : "共 16 项技能",
  "subItems" : null
}, {
  "item" : "自我介绍",
  "maxScore" : 5,
  "score" : 5,
  "message" : "自我介绍 131 字",
  "subItems" : null
}, {
  "item" : "岗位匹配度",
  "maxScore" : 15,
  "score" : 0,
  "message" : "目标岗位「求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望参与Web应用全生命周期开发、接口设计、前后端联调与高可用系统建设。」未在岗位字典中找到匹配，无法评估匹配度",
  "subItems" : null
} ]',NULL,'2026-09-02 11:30:54','/profile/upload/resume/2026/09/01/resume_1_d35855a2389d4f0a8871826a3e8a0881.pdf','2026-09-01 15:49:55','published','online',NULL,NULL,'','2026-08-31 10:09:20','','2026-09-02 11:31:32',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                      (6,'页共的简历',NULL,1,'页共',NULL,'2018-11-20','19987671567','19987671567@163.com',NULL,NULL,NULL,NULL,NULL,'[ {
  "name" : "Java",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Python",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Go",
  "level" : "了解",
  "category" : null
}, {
  "name" : "JavaScript",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Vue",
  "level" : "了解",
  "category" : null
}, {
  "name" : "React",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Spring Boot",
  "level" : "了解",
  "category" : null
}, {
  "name" : "MySQL",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Redis",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Kafka",
  "level" : "了解",
  "category" : null
}, {
  "name" : "RabbitMQ",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Docker",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Linux",
  "level" : "了解",
  "category" : null
}, {
  "name" : "MyBatis",
  "level" : "了解",
  "category" : null
} ]',NULL,'姓名：页共
出生日期：2018-11-20
电话：19987671567
邮箱：19987671567@163.com

【专业技能】
- Java（了解）
- Python（了解）
- Go（了解）
- JavaScript（了解）
- Vue（了解）
- React（了解）
- Spring Boot（了解）
- MySQL（了解）
- Redis（了解）
- Kafka（了解）
- RabbitMQ（了解）
- Docker（了解）
- Linux（了解）
- MyBatis（了解）',NULL,NULL,NULL,NULL,NULL,NULL,'draft','attachment','http://127.0.0.1:9001/moyun/2026/09/01/420a1affcc1848b0bb95d9105f918cd0.pdf','钟永国-boss在线简历-Java.pdf','','2026-09-01 16:37:46','','2026-09-01 16:37:46',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                      (6,'页共的简历',NULL,1,'页共',NULL,'2018-11-20','19987671567','19987671567@163.com',NULL,NULL,NULL,NULL,NULL,'[ {
  "name" : "Java",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Python",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Go",
  "level" : "了解",
  "category" : null
}, {
  "name" : "JavaScript",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Vue",
  "level" : "了解",
  "category" : null
}, {
  "name" : "React",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Spring Boot",
  "level" : "了解",
  "category" : null
}, {
  "name" : "MySQL",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Redis",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Kafka",
  "level" : "了解",
  "category" : null
}, {
  "name" : "RabbitMQ",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Docker",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Linux",
  "level" : "了解",
  "category" : null
}, {
  "name" : "MyBatis",
  "level" : "了解",
  "category" : null
} ]',NULL,'姓名：页共
出生日期：2018-11-20
电话：19987671567
邮箱：19987671567@163.com

【专业技能】
- Java（了解）
- Python（了解）
- Go（了解）
- JavaScript（了解）
- Vue（了解）
- React（了解）
- Spring Boot（了解）
- MySQL（了解）
- Redis（了解）
- Kafka（了解）
- RabbitMQ（了解）
- Docker（了解）
- Linux（了解）
- MyBatis（了解）',NULL,NULL,NULL,NULL,NULL,NULL,'draft','attachment','http://127.0.0.1:9001/moyun/2026/09/01/f362589317454f638cc5e36100323981.pdf','钟永国-boss在线简历-Java.pdf','','2026-09-01 16:38:44','','2026-09-01 16:38:44',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                      (6,'页共的简历',NULL,1,'页共',NULL,'2018-11-20','19987671567','19987671567@163.com',NULL,NULL,NULL,NULL,NULL,'[ {
  "name" : "Java",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Python",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Go",
  "level" : "了解",
  "category" : null
}, {
  "name" : "JavaScript",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Vue",
  "level" : "了解",
  "category" : null
}, {
  "name" : "React",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Spring Boot",
  "level" : "了解",
  "category" : null
}, {
  "name" : "MySQL",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Redis",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Kafka",
  "level" : "了解",
  "category" : null
}, {
  "name" : "RabbitMQ",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Docker",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Linux",
  "level" : "了解",
  "category" : null
}, {
  "name" : "MyBatis",
  "level" : "了解",
  "category" : null
} ]',NULL,'姓名：页共
出生日期：2018-11-20
电话：19987671567
邮箱：19987671567@163.com

【专业技能】
- Java（了解）
- Python（了解）
- Go（了解）
- JavaScript（了解）
- Vue（了解）
- React（了解）
- Spring Boot（了解）
- MySQL（了解）
- Redis（了解）
- Kafka（了解）
- RabbitMQ（了解）
- Docker（了解）
- Linux（了解）
- MyBatis（了解）',NULL,NULL,NULL,NULL,NULL,NULL,'draft','attachment','http://127.0.0.1:9001/moyun/2026/09/01/62b2e18f18354643bbbe1430dc284f80.pdf','钟永国-boss在线简历-Java.pdf','','2026-09-01 18:09:01','','2026-09-01 18:09:01',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                      (6,'钟永国-boss在线简历-Java',NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'draft','attachment','http://127.0.0.1:9001/moyun/2026/09/02/f5830e65f81146ab8e4cbc09ea7c16f5.pdf','钟永国-boss在线简历-Java.pdf','','2026-09-02 10:16:32','','2026-09-02 10:16:32',NULL,'0'),
                                                                                                                                                                                                                                                                                                                                                                                      (6,'钟永国的简历',NULL,1,'钟永国',NULL,'2018-11-20','19987671567','19987671567@163.com',NULL,NULL,NULL,NULL,NULL,'[ {
  "name" : "Java",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Python",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Go",
  "level" : "了解",
  "category" : null
}, {
  "name" : "JavaScript",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Vue",
  "level" : "了解",
  "category" : null
}, {
  "name" : "React",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Spring Boot",
  "level" : "了解",
  "category" : null
}, {
  "name" : "MySQL",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Redis",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Kafka",
  "level" : "了解",
  "category" : null
}, {
  "name" : "RabbitMQ",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Docker",
  "level" : "了解",
  "category" : null
}, {
  "name" : "Linux",
  "level" : "了解",
  "category" : null
}, {
  "name" : "MyBatis",
  "level" : "了解",
  "category" : null
} ]',NULL,'姓名：钟永国
出生日期：2018-11-20
电话：19987671567
邮箱：19987671567@163.com

【专业技能】
- Java（了解）
- Python（了解）
- Go（了解）
- JavaScript（了解）
- Vue（了解）
- React（了解）
- Spring Boot（了解）
- MySQL（了解）
- Redis（了解）
- Kafka（了解）
- RabbitMQ（了解）
- Docker（了解）
- Linux（了解）
- MyBatis（了解）',NULL,NULL,60,NULL,NULL,NULL,'draft','attachment','http://127.0.0.1:9001/moyun/2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf','钟永国-boss在线简历-Java.pdf','','2026-09-07 16:29:59','','2026-09-07 16:30:32',NULL,'0');
INSERT INTO `moyun-db`.portal_user_stats (user_id,article_count,article_view_sum,article_like_sum,article_bookmark_sum,article_word_sum,book_finished,booklist_count,quote_count,reading_minutes,question_solved,note_count,experience_count,note_adopted,follower_count,following_count,comment_count,total_like_received,checkin_streak,last_checkin_date,create_by,create_time,update_by,update_time,remark,weak_tags,weak_tags_updated_time) VALUES
                                                                                                                                                                                                                                                                                                                                                                                                                                                     (6,5,0,0,0,1745,0,0,0,0,10,0,30,0,0,1,0,0,1,'2026-09-07','','2026-08-28 09:58:10','','2026-09-07 17:08:33',NULL,NULL,NULL),
                                                                                                                                                                                                                                                                                                                                                                                                                                                     (7,2,0,2,6,7924,0,0,0,0,20,0,0,0,11,0,2,2,1,'2026-08-28','','2026-08-28 13:12:22','','2026-09-02 14:22:39',NULL,NULL,NULL),
                                                                                                                                                                                                                                                                                                                                                                                                                                                     (8,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,NULL,'','2026-08-28 13:36:33','','2026-08-28 13:36:33',NULL,NULL,NULL);
INSERT INTO `moyun-db`.portal_voice_interview (user_id,`position`,scene,resume_id,agent_id,phase,intro_score_json,status,`style`,difficulty,total_qa,current_idx,score,summary,report,share_token,share_expire_time,share_count,config_json,is_personalized,profile_snapshot,create_time,update_time,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                   (6,'Java 后端开发工程师','technical',1,NULL,NULL,NULL,'in_progress','professional','medium',5,0,NULL,NULL,NULL,NULL,NULL,0,'{
  "hintsEnabled" : true,
  "stuckThreshold" : 30,
  "style" : "professional",
  "difficulty" : "medium",
  "profileGaps" : [ ],
  "levelEstimate" : "",
  "questionIds" : [ "7", "3", "2", "6", "4" ]
}',0,'{
  "userId" : "6",
  "position" : "Java 后端开发工程师",
  "scene" : "technical",
  "requiredSkills" : [ ],
  "weakTags" : [ ],
  "personalized" : false
}','2026-08-31 10:16:19','2026-08-31 10:16:19','0'),
                                                                                                                                                                                                                                                                                                                   (6,'Java 后端开发工程师','technical',5,NULL,NULL,NULL,'finished','professional','medium',5,0,0,'本次面试共 5 题，作答 1 题。整体表现一般，建议针对薄弱点深入复习。 综合水平画像：初级（基础需夯实）。','{
  "interviewId" : "2",
  "totalScore" : 0,
  "dimensions" : {
    "coverage" : 0,
    "length" : 0,
    "structure" : 0,
    "relevance" : 0,
    "professionalism" : 0,
    "fluency" : 0
  },
  "highlights" : [ ],
  "weakPoints" : [ "HTTP与HTTPS的主要区别是什么？", "未说明HTTPS在HTTP基础上加入SSL/TLS加密。", "未提及HTTP默认端口80、HTTPS默认端口443。", "回答内容为不可识别语音噪音，无法评估技术表达能力。" ],
  "questionReviews" : [ {
    "questionIdx" : 0,
    "question" : "HTTP与HTTPS的主要区别是什么？",
    "score" : 0,
    "feedback" : "候选人至少尝试进行了回答。当前转写内容没有可识别的技术信息，未能说明HTTP与HTTPS的区别。建议从SSL/TLS加密、默认端口和CA证书角度重新组织答案。"
  } ],
  "summary" : "本次面试共 5 题，作答 1 题。整体表现一般，建议针对薄弱点深入复习。 综合水平画像：初级（基础需夯实）。",
  "suggestion" : "建议重点复习以下薄弱知识点：HTTP与HTTPS的主要区别是什么？、未说明HTTPS在HTTP基础上加入SSL/TLS加密。、未提及HTTP默认端口80、HTTPS默认端口443。、回答内容为不可识别语音噪音，无法评估技术表达能力。。可通过错题本针对性练习。"
}',NULL,NULL,0,'{
  "hintsEnabled" : true,
  "stuckThreshold" : 30,
  "style" : "professional",
  "difficulty" : "medium",
  "profileGaps" : [ "未说明HTTPS在HTTP基础上加入SSL/TLS加密。", "未提及HTTP默认端口80、HTTPS默认端口443。", "回答内容为不可识别语音噪音，无法评估技术表达能力。" ],
  "levelEstimate" : "junior",
  "questionIds" : [ "2", "3", "1", "8", "6" ]
}',0,'{
  "userId" : "6",
  "position" : "Java 后端开发工程师",
  "scene" : "technical",
  "requiredSkills" : [ ],
  "weakTags" : [ ],
  "personalized" : false
}','2026-09-04 17:20:47','2026-09-04 17:20:47','0'),
                                                                                                                                                                                                                                                                                                                   (6,'求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望','technical',5,47,'FINISHED',NULL,'finished','professional','medium',5,2,11,'本次面试共 5 题，作答 3 题。整体表现一般，建议针对薄弱点深入复习。 综合水平画像：初级（基础需夯实）。','{
  "interviewId" : "3",
  "totalScore" : 11,
  "dimensions" : {
    "coverage" : 0,
    "length" : 0,
    "structure" : 0,
    "relevance" : 5,
    "professionalism" : 33,
    "fluency" : 30,
    "logic" : 8,
    "confidence" : 13
  },
  "highlights" : [ ],
  "weakPoints" : [ "TCP三次握手的目的是什么？", "HTTP与HTTPS的主要区别是什么？", "进程和线程的本质区别是什么？", "未说明三次握手用于同步初始序列号", "未提及确认双方收发能力或建立可靠连接", "回答过于简短，缺乏任何技术信息", "未说明HTTP与HTTPS的核心区别。", "未提到TLS/SSL加密或证书验证。", "转写内容不连贯，无法判断技术理解。", "未说明进程和线程的基本定义", "未提到资源分配与CPU调度的区别" ],
  "questionReviews" : [ {
    "questionIdx" : 0,
    "question" : "TCP三次握手的目的是什么？",
    "score" : 4,
    "feedback" : "你能坦诚说明自己不了解，这点没问题。但回答没有涉及三次握手的目的、初始序列号同步或可靠连接基础，无法体现网络基础知识。"
  }, {
    "questionIdx" : 1,
    "question" : "HTTP与HTTPS的主要区别是什么？",
    "score" : 9,
    "feedback" : "从转写看可能是环境干扰，状态还算平稳。但这段内容没有回答问题，建议直接说明HTTPS通过TLS/SSL加密、证书验证和默认端口差异。"
  }, {
    "questionIdx" : 2,
    "question" : "进程和线程的本质区别是什么？",
    "score" : 20,
    "feedback" : "你愿意继续回答，但目前还没有说明进程和线程的区别。建议从进程是资源分配单位、线程是CPU调度单位来补充。"
  } ],
  "summary" : "本次面试共 5 题，作答 3 题。整体表现一般，建议针对薄弱点深入复习。 综合水平画像：初级（基础需夯实）。",
  "suggestion" : "建议重点复习以下薄弱知识点：TCP三次握手的目的是什么？、HTTP与HTTPS的主要区别是什么？、进程和线程的本质区别是什么？、未说明三次握手用于同步初始序列号、未提及确认双方收发能力或建立可靠连接、回答过于简短，缺乏任何技术信息、未说明HTTP与HTTPS的核心区别。、未提到TLS/SSL加密或证书验证。、转写内容不连贯，无法判断技术理解。、未说明进程和线程的基本定义、未提到资源分配与CPU调度的区别。可通过错题本针对性练习。",
  "sentimentTrend" : [ "calm", "hesitant", "hesitant" ],
  "redFlags" : [ "答非所问，内容与问题无关", "回答同义反复，未提供有效信息" ],
  "fluencyAvg" : 30,
  "introScore" : null,
  "improvementSuggestions" : [ "针对薄弱点「TCP三次握手的目的是什么？」做专项复习，可结合错题本巩固。", "针对薄弱点「HTTP与HTTPS的主要区别是什么？」做专项复习，可结合错题本巩固。", "针对薄弱点「进程和线程的本质区别是什么？」做专项复习，可结合错题本巩固。" ]
}',NULL,NULL,0,'{
  "hintsEnabled" : true,
  "stuckThreshold" : 30,
  "style" : "professional",
  "difficulty" : "medium",
  "questionMode" : "dynamic",
  "sceneConfigId" : "1",
  "interviewConfigId" : "1",
  "enableSelfIntro" : false,
  "maxFollowups" : 2,
  "profileGaps" : [ "未说明三次握手用于同步初始序列号", "未提及确认双方收发能力或建立可靠连接", "回答过于简短，缺乏任何技术信息", "未说明HTTP与HTTPS的核心区别。", "未提到TLS/SSL加密或证书验证。", "转写内容不连贯，无法判断技术理解。", "未说明进程和线程的基本定义", "未提到资源分配与CPU调度的区别" ],
  "levelEstimate" : "junior"
}',0,'{
  "userId" : "6",
  "position" : "求职意向：全栈工程师 / Java后端开发工程师。聚焦Java、Spring Boot、MySQL、Vue与微服务架构方向，期望",
  "scene" : "technical",
  "requiredSkills" : [ ],
  "weakTags" : [ ],
  "personalized" : false
}','2026-09-07 16:30:24','2026-09-07 16:30:24','0'),
                                                                                                                                                                                                                                                                                                                   (6,'Java 后端开发','technical',6,47,'TECH_QUESTION','{
  "dimensions" : {
    "structure" : 75,
    "awareness" : 70,
    "matching" : 55,
    "fluency" : 70
  },
  "total" : 68,
  "comment" : "自我介绍内容基本完整，建议进一步突出与目标岗位匹配的项目亮点。",
  "strengths" : [ ],
  "weaknesses" : [ ],
  "followupWorth" : false,
  "followupQuestion" : null
}','in_progress','professional','medium',5,3,NULL,NULL,NULL,NULL,NULL,0,'{
  "hintsEnabled" : true,
  "stuckThreshold" : 30,
  "style" : "professional",
  "difficulty" : "medium",
  "questionMode" : "dynamic",
  "sceneConfigId" : "1",
  "interviewConfigId" : "2",
  "enableSelfIntro" : true,
  "maxFollowups" : 4,
  "profileGaps" : [ "两个事务分别更新不同表，但未解释完整锁链路如何形成循环等待。", "统一加锁顺序没有说明是否覆盖异步任务、批量更新或其他入口。", "缺少修复后的量化压测结果或死锁复发验证。", "事务B在等待锁时又触发新锁需求的描述不符合事务阻塞后的执行模型。", "UPDATE父表status必然触发子表加锁的说法过度泛化，未说明外键检查条件。", "未明确事务A最终等待哪把锁，导致循环等待链路没有闭合。", "未明确回答InnoDB是否支持锁升级，以及所谓升级对应哪条SQL。", "把事务B等待排他锁描述为“隐式锁”，混淆了锁持有与锁等待。" ],
  "levelEstimate" : "junior"
}',0,'{
  "userId" : "6",
  "position" : "Java 后端开发",
  "scene" : "technical",
  "requiredSkills" : [ ],
  "weakTags" : [ ],
  "personalized" : false
}','2026-09-07 16:44:35','2026-09-07 16:44:35','0'),
                                                                                                                                                                                                                                                                                                                   (6,'Java 后端开发','technical',6,47,'FINISHED','{
  "dimensions" : {
    "structure" : 40,
    "awareness" : 45,
    "matching" : 55,
    "fluency" : 45
  },
  "total" : 46,
  "comment" : "自我介绍偏短，建议补充技术栈、项目亮点与求职方向。",
  "strengths" : [ ],
  "weaknesses" : [ ],
  "followupWorth" : false,
  "followupQuestion" : null
}','finished','professional','medium',5,2,25,'本次面试共 5 题，作答 5 题。整体表现一般，建议针对薄弱点深入复习。 综合水平画像：初级（基础需夯实）。','{
  "interviewId" : "5",
  "totalScore" : 25,
  "dimensions" : {
    "coverage" : 0,
    "length" : 0,
    "structure" : 8,
    "awareness" : 9,
    "matching" : 11,
    "fluency" : 45,
    "relevance" : 4,
    "professionalism" : 10,
    "logic" : 14,
    "confidence" : 24
  },
  "highlights" : [ ],
  "weakPoints" : [ "请先做一个约3分钟的自我介绍，内容包括你的基本情况、技术栈、项目经历和求职方向。", "TCP三次握手的目的是什么？", "你刚提到“旭林知行”，这个和TCP三次握手有直接关系吗？如果没有，请先回答：TCP三次握手最核心的目的是什么？", "进程和线程的本质区别是什么？", "HTTP与HTTPS的主要区别是什么？", "回答内容为个人项目介绍，与TCP三次握手问题完全无关", "未提及任何关于序列号、连接建立或可靠传输的技术概念", "对基础网络问题缺乏回应，可能暴露知识盲区或未听清题目", "回答内容为个人项目介绍，与TCP三次握手问题完全无关。", "疑似背诵事先准备好的项目材料，未理解面试官提问。", "未展示任何TCP协议相关基础知识。", "无法回答进程与线程的本质区别，暴露操作系统基础概念缺失", "未正面回答HTTP与HTTPS的区别，回答与问题无关" ],
  "questionReviews" : [ {
    "questionIdx" : -1,
    "question" : "请先做一个约3分钟的自我介绍，内容包括你的基本情况、技术栈、项目经历和求职方向。",
    "score" : 46,
    "feedback" : "自我介绍偏短，建议补充技术栈、项目亮点与求职方向。"
  }, {
    "questionIdx" : 0,
    "question" : "TCP三次握手的目的是什么？",
    "score" : 15,
    "feedback" : "能主动谈及个人项目值得肯定，但当前问题是TCP三次握手的目的，你的回答完全未涉及任何技术要点。"
  }, {
    "questionIdx" : 0,
    "question" : "你刚提到“旭林知行”，这个和TCP三次握手有直接关系吗？如果没有，请先回答：TCP三次握手最核心的目的是什么？",
    "score" : 17,
    "feedback" : "你介绍项目时表达较流畅，但这次回答完全没涉及TCP三次握手的目的，属于答非所问。"
  }, {
    "questionIdx" : 1,
    "question" : "进程和线程的本质区别是什么？",
    "score" : 7,
    "feedback" : "能坦诚说不会也算真诚，但操作系统最基础的概念需要尽快补上。"
  }, {
    "questionIdx" : 2,
    "question" : "HTTP与HTTPS的主要区别是什么？",
    "score" : 14,
    "feedback" : "能感觉到你有些情绪，沟通态度需要调整；这次没有回答HTTP和HTTPS的区别，基础网络知识需要复习。"
  } ],
  "summary" : "本次面试共 5 题，作答 5 题。整体表现一般，建议针对薄弱点深入复习。 综合水平画像：初级（基础需夯实）。",
  "suggestion" : "建议重点复习以下薄弱知识点：请先做一个约3分钟的自我介绍，内容包括你的基本情况、技术栈、项目经历和求职方向。、TCP三次握手的目的是什么？、你刚提到“旭林知行”，这个和TCP三次握手有直接关系吗？如果没有，请先回答：TCP三次握手最核心的目的是什么？、进程和线程的本质区别是什么？、HTTP与HTTPS的主要区别是什么？、回答内容为个人项目介绍，与TCP三次握手问题完全无关、未提及任何关于序列号、连接建立或可靠传输的技术概念、对基础网络问题缺乏回应，可能暴露知识盲区或未听清题目、回答内容为个人项目介绍，与TCP三次握手问题完全无关。、疑似背诵事先准备好的项目材料，未理解面试官提问。、未展示任何TCP协议相关基础知识。、无法回答进程与线程的本质区别，暴露操作系统基础概念缺失、未正面回答HTTP与HTTPS的区别，回答与问题无关。可通过错题本针对性练习。",
  "sentimentTrend" : [ "hesitant", "nervous", "nervous", "nervous" ],
  "redFlags" : [ "答非所问，疑似未听清或刻意回避技术问题", "答非所问：候选人在被问基础网络问题时抛出一段项目背景，疑似未认真审题或背诵。", "背诵痕迹：大段连贯的项目描述具有明显准备痕迹，缺少临场组织。", "答非所问，且带有抵触情绪", "回答内容为零，疑似放弃或不愿继续" ],
  "fluencyAvg" : 48,
  "introScore" : {
    "dimensions" : {
      "structure" : 40,
      "awareness" : 45,
      "matching" : 55,
      "fluency" : 45
    },
    "total" : 46,
    "comment" : "自我介绍偏短，建议补充技术栈、项目亮点与求职方向。",
    "strengths" : [ ],
    "weaknesses" : [ ]
  },
  "improvementSuggestions" : [ "针对薄弱点「请先做一个约3分钟的自我介绍，内容包括你的基本情况、技术栈、项目经历和求职方向。」做专项复习，可结合错题本巩固。", "针对薄弱点「TCP三次握手的目的是什么？」做专项复习，可结合错题本巩固。", "针对薄弱点「你刚提到“旭林知行”，这个和TCP三次握手有直接关系吗？如果没有，请先回答：TCP三次握手最核心的目的是什么？」做专项复习，可结合错题本巩固。" ],
  "knowledgePoints" : [ {
    "title" : "TCP",
    "desc" : "本场面试出现 4 次（高频/薄弱点，建议优先巩固）"
  }, {
    "title" : "网络",
    "desc" : "本场面试出现 4 次（高频/薄弱点，建议优先巩固）"
  }, {
    "title" : "三次握手",
    "desc" : "本场面试出现 4 次（高频/薄弱点，建议优先巩固）"
  }, {
    "title" : "进程",
    "desc" : "本场面试出现 2 次（高频/薄弱点，建议优先巩固）"
  }, {
    "title" : "线程",
    "desc" : "本场面试出现 2 次（高频/薄弱点，建议优先巩固）"
  }, {
    "title" : "操作系统",
    "desc" : "本场面试出现 2 次（高频/薄弱点，建议优先巩固）"
  }, {
    "title" : "HTTP",
    "desc" : "本场面试出现 2 次（高频/薄弱点，建议优先巩固）"
  }, {
    "title" : "HTTPS",
    "desc" : "本场面试出现 2 次（高频/薄弱点，建议优先巩固）"
  } ]
}',NULL,NULL,0,'{
  "hintsEnabled" : true,
  "stuckThreshold" : 30,
  "style" : "professional",
  "difficulty" : "medium",
  "questionMode" : "dynamic",
  "sceneConfigId" : "1",
  "interviewConfigId" : "2",
  "enableSelfIntro" : true,
  "maxFollowups" : 4,
  "profileGaps" : [ "回答内容为个人项目介绍，与TCP三次握手问题完全无关", "未提及任何关于序列号、连接建立或可靠传输的技术概念", "对基础网络问题缺乏回应，可能暴露知识盲区或未听清题目", "回答内容为个人项目介绍，与TCP三次握手问题完全无关。", "疑似背诵事先准备好的项目材料，未理解面试官提问。", "未展示任何TCP协议相关基础知识。", "无法回答进程与线程的本质区别，暴露操作系统基础概念缺失", "未正面回答HTTP与HTTPS的区别，回答与问题无关" ],
  "levelEstimate" : "junior"
}',0,'{
  "userId" : "6",
  "position" : "Java 后端开发",
  "scene" : "technical",
  "requiredSkills" : [ ],
  "weakTags" : [ ],
  "personalized" : false
}','2026-09-11 16:23:35','2026-09-11 16:23:34','0');
INSERT INTO `moyun-db`.portal_voice_interview_qa (interview_id,question_id,question_source,question_idx,parent_qa_id,question,user_answer,transcription_edited,ai_feedback,speak_text,score,rule_dimensions_json,llm_score_json,llm_analysis_json,hint_used,latency_ms,next_action,create_time,update_time,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                         (1,7,NULL,0,NULL,'反转链表',NULL,0,NULL,'你好，欢迎参加Java 后端开发工程师岗位的语音面试。我是你的面试官。第一题：反转链表',NULL,NULL,NULL,NULL,0,NULL,NULL,'2026-08-31 10:16:19','2026-08-31 10:16:19','0'),
                                                                                                                                                                                                                                                                                                                         (2,2,NULL,0,NULL,'HTTP与HTTPS的主要区别是什么？','爱丁堡v发v大va撒打发',0,'候选人至少尝试进行了回答。当前转写内容没有可识别的技术信息，未能说明HTTP与HTTPS的区别。建议从SSL/TLS加密、默认端口和CA证书角度重新组织答案。','没听清，请重新回答：HTTP 与 HTTPS 的区别，可提示加密、证书、端口。',0,'{
  "relevance" : 0,
  "professionalism" : 0,
  "fluency" : 0
}',NULL,NULL,0,12318,'followup','2026-09-04 17:20:47','2026-09-04 17:20:46','0'),
                                                                                                                                                                                                                                                                                                                         (2,2,NULL,0,2,'你提到了一些做法，能展开讲讲「爱丁堡v发v大va撒打发」这个点吗？',NULL,0,NULL,'我注意到你刚才的回答里，未说明HTTPS在HTTP基础上加入SSL/TLS加密。 这一点说得还比较模糊。你提到了一些做法，能展开讲讲「爱丁堡v发v大va撒打发」这个点吗？',NULL,NULL,NULL,NULL,0,NULL,NULL,'2026-09-04 17:21:49','2026-09-04 17:21:48','0'),
                                                                                                                                                                                                                                                                                                                         (3,1,'bank',0,NULL,'TCP三次握手的目的是什么？','好的我不知道哈',0,'你能坦诚说明自己不了解，这点没问题。但回答没有涉及三次握手的目的、初始序列号同步或可靠连接基础，无法体现网络基础知识。','没关系，不知道可以直接说。我们换个基础点的问题。',4,'{
  "relevance" : 10,
  "professionalism" : 50,
  "fluency" : 60,
  "logic" : 20,
  "confidence" : 20
}',NULL,'{
  "reply" : "没关系，不知道可以直接说。我们换个基础点的问题。",
  "score" : 5,
  "dimensions" : {
    "relevance" : 10,
    "professionalism" : 50,
    "fluency" : 60,
    "logic" : 20,
    "confidence" : 20
  },
  "feedback" : "你能坦诚说明自己不了解，这点没问题。但回答没有涉及三次握手的目的、初始序列号同步或可靠连接基础，无法体现网络基础知识。",
  "flaws" : [ "未说明三次握手用于同步初始序列号", "未提及确认双方收发能力或建立可靠连接", "回答过于简短，缺乏任何技术信息" ],
  "redFlags" : [ ],
  "sentiment" : {
    "state" : "calm",
    "note" : "候选人直接承认不知道，语气平和且无明显紧张。"
  },
  "fluencyAssessment" : {
    "score" : 60,
    "comment" : "语句很短但顺畅，带有口语化语气词，无明显停顿或重复。"
  },
  "completeness" : {
    "covered" : [ ],
    "missing" : [ "同步初始序列号", "确认双方收发能力", "建立可靠连接基础" ]
  },
  "level" : "junior",
  "followupWorth" : false,
  "nextAction" : "change_topic",
  "nextQuestion" : "HTTP与HTTPS的主要区别是什么？",
  "candidateId" : null,
  "transition" : "没关系，我们换一个基础问题，你说说HTTP和HTTPS的区别。",
  "guidance" : ""
}',2,7576,'next','2026-09-07 16:30:35','2026-09-07 16:30:35','0'),
                                                                                                                                                                                                                                                                                                                         (3,NULL,'llm',1,NULL,'HTTP与HTTPS的主要区别是什么？','结束啊到底谁把我跟人家又有。',0,'从转写看可能是环境干扰，状态还算平稳。但这段内容没有回答问题，建议直接说明HTTPS通过TLS/SSL加密、证书验证和默认端口差异。','没关系，这段我没听清，我们先进入下一题。',9,'{
  "relevance" : 0,
  "professionalism" : 30,
  "fluency" : 10,
  "logic" : 0,
  "confidence" : 10
}',NULL,'{
  "reply" : "没关系，这段我没听清，我们先进入下一题。",
  "score" : 5,
  "dimensions" : {
    "relevance" : 0,
    "professionalism" : 30,
    "fluency" : 10,
    "logic" : 0,
    "confidence" : 10
  },
  "feedback" : "从转写看可能是环境干扰，状态还算平稳。但这段内容没有回答问题，建议直接说明HTTPS通过TLS/SSL加密、证书验证和默认端口差异。",
  "flaws" : [ "未说明HTTP与HTTPS的核心区别。", "未提到TLS/SSL加密或证书验证。", "转写内容不连贯，无法判断技术理解。" ],
  "redFlags" : [ "答非所问，内容与问题无关" ],
  "sentiment" : {
    "state" : "hesitant",
    "note" : "语句破碎且无有效信息，显得犹豫或受干扰。"
  },
  "fluencyAssessment" : {
    "score" : 10,
    "comment" : "存在明显不连贯和无效语句，几乎没有有效表达。"
  },
  "completeness" : {
    "covered" : [ ],
    "missing" : [ "HTTP与HTTPS基本概念", "TLS/SSL加密作用", "证书或身份验证", "端口或安全性差异" ]
  },
  "level" : "junior",
  "followupWorth" : false,
  "nextAction" : "change_topic",
  "nextQuestion" : "进程和线程的本质区别是什么？",
  "candidateId" : "3",
  "transition" : "这个问题先放一下，我们看下一题：进程和线程的本质区别是什么？",
  "guidance" : "可以只从HTTP明文、HTTPS加密和证书验证来回答。"
}',0,6760,'next','2026-09-07 16:32:16','2026-09-07 16:32:15','0'),
                                                                                                                                                                                                                                                                                                                         (3,3,'bank',2,NULL,'进程和线程的本质区别是什么？','进程是进程，线程是线程，
',0,'你愿意继续回答，但目前还没有说明进程和线程的区别。建议从进程是资源分配单位、线程是CPU调度单位来补充。','没关系，你可以从资源分配和CPU调度这两个角度说说。',20,'{
  "relevance" : 5,
  "professionalism" : 20,
  "fluency" : 20,
  "logic" : 5,
  "confidence" : 10
}',NULL,'{
  "reply" : "没关系，你可以从资源分配和CPU调度这两个角度说说。",
  "score" : 5,
  "dimensions" : {
    "relevance" : 5,
    "professionalism" : 20,
    "fluency" : 20,
    "logic" : 5,
    "confidence" : 10
  },
  "feedback" : "你愿意继续回答，但目前还没有说明进程和线程的区别。建议从进程是资源分配单位、线程是CPU调度单位来补充。",
  "flaws" : [ "未说明进程和线程的基本定义", "未提到资源分配与CPU调度的区别", "未涉及共享资源、通信或同步" ],
  "redFlags" : [ "回答同义反复，未提供有效信息" ],
  "sentiment" : {
    "state" : "hesitant",
    "note" : "回答只重复概念，缺乏展开，显得不确定。"
  },
  "fluencyAssessment" : {
    "score" : 20,
    "comment" : "存在重复和语义中断，表达不完整。"
  },
  "completeness" : {
    "covered" : [ ],
    "missing" : [ "进程是资源分配的最小单位", "线程是CPU调度的最小单位", "线程共享进程资源且通信成本低、需要同步" ]
  },
  "level" : "junior",
  "followupWorth" : true,
  "nextAction" : "deepen",
  "nextQuestion" : "你刚说“进程是进程，线程是线程”，那它们在资源分配和CPU调度上分别是什么单位？",
  "candidateId" : null,
  "transition" : "我们顺着这个点，再从资源分配和调度补充一下。",
  "guidance" : "请从资源分配和CPU调度两个角度说明区别。"
}',0,23489,'followup','2026-09-07 16:33:25','2026-09-07 16:33:24','0'),
                                                                                                                                                                                                                                                                                                                         (3,3,'llm',2,6,'你刚说“进程是进程，线程是线程”，那它们在资源分配和CPU调度上分别是什么单位？',NULL,0,NULL,'我注意到你刚才的回答里，未说明进程和线程的基本定义 这一点说得还比较模糊。你刚说“进程是进程，线程是线程”，那它们在资源分配和CPU调度上分别是什么单位？',NULL,NULL,NULL,NULL,0,NULL,NULL,'2026-09-07 16:34:50','2026-09-07 16:34:49','0'),
                                                                                                                                                                                                                                                                                                                         (4,NULL,'self_intro',-1,NULL,'请先做一个约3分钟的自我介绍，内容包括你的基本情况、技术栈、项目经历和求职方向。','各位面试官好，我叫钟永国，30岁，本科学历，2018年毕业，至今有8年Java开发经验。

我早期接触的是传统企业级开发，做过贵州定制之家商城、厦门农商银行电子账户、平安银行智慧监管报送系统等项目。那时候更多是在解决业务问题——怎么写代码、怎么把系统交付出去。真正让我对技术产生质变是在2022年之后，我接触到微服务、容器化和AI工程化方向，开始有意识地往这个方向走。

我的技术栈可以分三层说：

第一层是基础设施层，Java 21 + Spring Boot 3 + Spring Cloud，MySQL + Redis + MinIO，Docker + K8s，这套组合我用了四年多，算是比较熟了。

第二层是AI工程化层，这部分是我最近一年重点投入的方向。LangChain4j、向量数据库、RAG检索、工作流编排、Agent设计，我都有落地经验。

第三层是工程化能力，双认证体系、幂等设计、容器化部署、灰度发布策略，不是停留在知道概念，是真正写在代码里、跑在服务器上的。

项目经历方面，我重点说一下这两年最有代表性的一个——旭林知行。

这是一个AI驱动的求职成长平台，覆盖简历诊断、AI面试、题库刷题、面经分享四个模块。整个项目从零开始，我一个人前后做了半年多。不是用脚手架搭完就结束，是需要落地到真实业务、被真实用户使用的。这里面有几个技术点值得提一下：

一个是AI面试流程的动态配置化。面试有6个阶段，自我介绍→追问→技术题→项目深挖→系统设计→候选人反问，每个阶段绑定的模型、知识库、提示词都可在后台动态配置，不用改代码、不用重启。

另一个是QuestionPicker智能出题器。四路题源——岗位核心题40%、简历深挖题30%、薄弱点题20%、随机兜底10%，出题权重可配，题量不够时会自动降级流转到其他题源。

还有一个是结构化评分，LLM评分占70%权重，规则引擎占30%。用户超短回答、关键词零命中会触发规则校验，防止LLM幻觉。

在项目中，我花了不少时间打磨上线后的细节。比如引入LanceDB替换了Elasticsearch做向量检索，内存占用从8GB降到了1GB以内。也一直坚持DevOps理念，用Docker Compose做统一部署、管理配置文件，尽可能让项目保持清晰、可维护的状态。

关于求职方向，我想往AI工程化方向走，具体来说就是AI Agent的设计与落地、RAG系统的优化、或者大模型与业务场景的结合。这应该是我接下来三到五年想做的事情。

最后，我性格上不属于那种特别外放的，但在技术方面比较踏实，遇到问题会自己先钻研，也不排斥跟团队一起讨论。希望有机会加入团队，在这个方向上继续深入。

以上就是我的自我介绍，谢谢。',0,'自我介绍内容基本完整，建议进一步突出与目标岗位匹配的项目亮点。','自我介绍内容基本完整，建议进一步突出与目标岗位匹配的项目亮点。',68,'{
  "structure" : 75,
  "awareness" : 70,
  "matching" : 55,
  "fluency" : 70
}',NULL,NULL,0,NULL,'next','2026-09-07 16:44:35','2026-09-07 16:44:35','0'),
                                                                                                                                                                                                                                                                                                                         (4,NULL,'llm',0,NULL,'请结合你简历中的项目经历，说明你是如何定位并解决死锁问题的？','在一次高并发压测中，我负责的“旭林知行”平台AI面试模块出现了服务卡顿，最终定位到是数据库死锁导致的。

当时系统表现是部分面试会话无法正常提交答案，最终超时。我定位问题的步骤是这样的：

分析应用日志与监控：首先检查了应用日志，发现大量 Deadlock found when trying to get lock; try restarting transaction 的异常堆栈。同时，系统的数据库监控面板显示，InnoDB 的行锁等待时间在异常时间点急剧升高。

使用 SHOW ENGINE INNODB STATUS 分析：立刻在MySQL中执行了 SHOW ENGINE INNODDB STATUS。通过查看最近一次死锁的日志，我清晰地看到了两个事务都在等待对方释放锁，形成了循环等待。

定位问题SQL：从死锁日志里，我定位到了两个相互冲突的SQL语句。事务A在执行 UPDATE voice_interview_qa SET score = ? WHERE id = ? AND interview_id = ?，而事务B在执行 UPDATE voice_interview SET status = ''FINISHED'' WHERE id = ?，它们分别锁定了 voice_interview_qa 和 voice_interview 这两张有外键关联的表。

找到根源后，我从以下几个方面解决了问题：

统一加锁顺序：排查发现，这两个更新操作在代码中的执行顺序不一致。我重构了相关Service，确保所有对这两张表的更新操作都遵循“先更新主表 voice_interview，再更新子表 voice_interview_qa”的统一顺序，从根本上破坏了循环等待的条件。

优化事务粒度：检查了相关事务代码，发现原本的事务范围过大，将多个非必要的查询也包在了事务中。我将事务边界缩小到只包含关键的更新操作，缩短了锁的持有时间。

完善监控告警：事后，我在数据库配置中开启了 innodb_print_all_deadlocks 参数，将所有死锁日志输出到错误日志文件，并建立了相应的监控告警，确保未来任何死锁问题都能被第一时间发现。

通过这次排查，我也沉淀了一套标准的排查流程：查日志 → 分析死锁 → 找根因 → 改代码 → 加监控。解决了这个问题后，AI面试模块在高并发下的稳定性提升了不少，后续也没有再出现过同类问题。',0,'回答能结合项目给出从日志、死锁分析到修复和监控的闭环，工程意识不错。不过两个事务如何形成循环等待的细节还不够扎实，建议补充完整事务、SQL、索引和锁模式。修复效果也最好用压测指标验证。','不错，排查闭环挺完整。我想再抠一下，两个事务到底怎么形成循环等待的。',81,'{
  "relevance" : 95,
  "professionalism" : 88,
  "fluency" : 88,
  "logic" : 87,
  "confidence" : 90
}',NULL,'{
  "reply" : "不错，排查闭环挺完整。我想再抠一下，两个事务到底怎么形成循环等待的。",
  "score" : 86,
  "dimensions" : {
    "relevance" : 95,
    "professionalism" : 88,
    "fluency" : 88,
    "logic" : 87,
    "confidence" : 90
  },
  "feedback" : "回答能结合项目给出从日志、死锁分析到修复和监控的闭环，工程意识不错。不过两个事务如何形成循环等待的细节还不够扎实，建议补充完整事务、SQL、索引和锁模式。修复效果也最好用压测指标验证。",
  "flaws" : [ "两个事务分别更新不同表，但未解释完整锁链路如何形成循环等待。", "统一加锁顺序没有说明是否覆盖异步任务、批量更新或其他入口。", "缺少修复后的量化压测结果或死锁复发验证。" ],
  "redFlags" : [ ],
  "sentiment" : {
    "state" : "confident",
    "note" : "排查步骤和解决措施表述完整，语气较稳。"
  },
  "fluencyAssessment" : {
    "score" : 88,
    "comment" : "整体表达流畅，逻辑清楚，仅有轻微术语口误和模板化表达。"
  },
  "completeness" : {
    "covered" : [ "故障现象", "日志与监控定位", "SHOW ENGINE INNODB STATUS分析", "统一加锁顺序", "缩小事务粒度", "死锁监控告警" ],
    "missing" : [ "完整事务执行顺序和锁等待链路", "具体索引、锁模式或行锁细节", "修复后的压测验证和量化结果" ]
  },
  "level" : "senior",
  "followupWorth" : true,
  "nextAction" : "deepen",
  "nextQuestion" : "你提到“两个事务都在等待对方释放锁”，请把这两个事务的完整执行顺序、涉及SQL以及锁住的索引或行具体讲一下？",
  "candidateId" : null,
  "transition" : "",
  "guidance" : ""
}',1,32248,'followup','2026-09-07 16:46:15','2026-09-07 16:46:15','0'),
                                                                                                                                                                                                                                                                                                                         (4,NULL,'llm',0,9,'你提到“两个事务都在等待对方释放锁”，请把这两个事务的完整执行顺序、涉及SQL以及锁住的索引或行具体讲一下？','假设两个事务几乎同时开始：

T1 时刻 — 事务 A 先执行

事务 A 执行 UPDATE voice_interview_qa SET score = 85 WHERE id = 1001 AND interview_id = 10：

需要锁住 voice_interview_qa 表上 id=1001 这一行（排他锁）

关键点：因为 interview_id 是外键，MySQL 需要确保 interview_id=10 在父表 voice_interview 中确实存在且未被删除

为了验证这一点，事务 A 会在 voice_interview 表上对 id=10 这一行加一个共享锁（S锁）

此时锁状态：

事务 A 持有：QA 表行锁 + Interview 表共享锁

事务 A 等待：无

T2 时刻 — 事务 B 执行（几乎同时）

事务 B 执行 UPDATE voice_interview SET status = ''FINISHED'' WHERE id = 10：

需要锁住 voice_interview 表上 id=10 这一行（排他锁）

但这一行已经被事务 A 加了共享锁，共享锁和排他锁不兼容

所以事务 B 被阻塞，等待事务 A 释放 Interview 表的共享锁

此时锁状态：

事务 A：持有 Interview 共享锁 → 等待？（还没等待）

事务 B：等待 Interview 排他锁

到这里还没死锁，只是阻塞。死锁需要形成“循环等待”。

T3 时刻 — 事务 B 在等待过程中，触发了新的锁需求

事务 B 在等待的过程中，MySQL 需要为 UPDATE 准备资源。因为 voice_interview_qa 表上有外键指向 voice_interview.id，更新 voice_interview 表时，为了维护外键约束的一致性，MySQL 需要检查所有子表（voice_interview_qa）中是否有 interview_id=10 的记录。

此时事务 B 需要对 voice_interview_qa 表中 interview_id=10 的所有行加锁（意向锁或共享锁）来执行一致性检查。

但是：事务 A 已经持有了 voice_interview_qa 表中 id=1001 这一行的排他锁（因为它刚更新了那行）。

于是：

事务 A 持有 QA 表行锁 → 需要 Interview 表共享锁（已完成）

事务 B 持有 Interview 表排他锁（正在等待）→ 需要 QA 表锁（被事务 A 持有）

循环等待形成：

text
事务 A 持有 QA 表锁 → 等待 Interview 表排他锁？（不是，A持有共享锁，它不等待）
等等，让我修正一下。实际情况是：

更精确的死锁情景：

事务 B 在等待 Interview 表排他锁的期间，为了准备后续操作，MySQL 尝试提前获取 voice_interview_qa 表上的锁（为了外键检查或后续操作），但被事务 A 阻塞。

与此同时，事务 A 虽然在 T1 已经拿到了 Interview 表的共享锁，但如果事务 A 后续还需要把共享锁升级为排他锁（比如更新 voice_interview 表的其他字段），而升级锁需要等待事务 B 释放它的排他锁（虽然 B 还没拿到），这就形成了循环等待。',0,'你能按时间线拆解事务，并意识到外键会引入父子表锁冲突，这点不错。但死锁闭环没有成立，事务等待期间继续获取锁、锁升级的说法需要更严谨。','可以，但你这个闭环还没闭合，重点讲清A最后到底等哪把锁。',63,'{
  "relevance" : 85,
  "professionalism" : 58,
  "fluency" : 60,
  "logic" : 45,
  "confidence" : 52
}',NULL,'{
  "reply" : "可以，但你这个闭环还没闭合，重点讲清A最后到底等哪把锁。",
  "score" : 61,
  "dimensions" : {
    "relevance" : 85,
    "professionalism" : 58,
    "fluency" : 60,
    "logic" : 45,
    "confidence" : 52
  },
  "feedback" : "你能按时间线拆解事务，并意识到外键会引入父子表锁冲突，这点不错。但死锁闭环没有成立，事务等待期间继续获取锁、锁升级的说法需要更严谨。",
  "flaws" : [ "事务B在等待锁时又触发新锁需求的描述不符合事务阻塞后的执行模型。", "UPDATE父表status必然触发子表加锁的说法过度泛化，未说明外键检查条件。", "未明确事务A最终等待哪把锁，导致循环等待链路没有闭合。" ],
  "redFlags" : [ "死锁链路前后修正，出现“事务A不等待”与“事务A升级锁等待”的矛盾。" ],
  "sentiment" : {
    "state" : "hesitant",
    "note" : "回答中出现“等等，让我修正一下”，说明对死锁链路不够确定。"
  },
  "fluencyAssessment" : {
    "score" : 60,
    "comment" : "有分步表达，但中途自我修正和重复较多，连贯性受到影响。"
  },
  "completeness" : {
    "covered" : [ "给出了两个事务的时序拆分", "提到了父子表外键和S/X锁", "描述了死锁日志定位方向" ],
    "missing" : [ "未给出真实死锁日志中的索引名、锁模式和锁类型", "未说明事务A最终等待事务B的哪把锁", "未说明隔离级别和更新字段对外键检查锁的影响" ]
  },
  "level" : "mid",
  "followupWorth" : true,
  "nextAction" : "deepen",
  "nextQuestion" : "你说“事务 A 后续还需要把共享锁升级为排他锁”，请说明对应哪条SQL、为什么会被B阻塞，以及InnoDB是否支持这种锁升级。",
  "candidateId" : null,
  "transition" : "",
  "guidance" : "请按真实死锁日志说明A和B分别持有、等待哪把索引锁。"
}',1,15786,'followup','2026-09-07 16:47:47','2026-09-07 16:47:47','0');
INSERT INTO `moyun-db`.portal_voice_interview_qa (interview_id,question_id,question_source,question_idx,parent_qa_id,question,user_answer,transcription_edited,ai_feedback,speak_text,score,rule_dimensions_json,llm_score_json,llm_analysis_json,hint_used,latency_ms,next_action,create_time,update_time,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                         (4,NULL,'llm',0,10,'你说“事务 A 后续还需要把共享锁升级为排他锁”，请说明对应哪条SQL、为什么会被B阻塞，以及InnoDB是否支持这种锁升级。','先说结论：事务 A 等的不是父表的排他锁，而是事务 B 在父表上持有的“隐式锁”
这是整个问题的核心。我之前说是“升级锁”，不精确。真正的情况是：事务 A 在尝试插入新的 QA 记录时，被事务 B 的隐式锁卡住了。

完整的死锁闭环（四步走，步步明确）
假设场景：同一场面试（interview_id = 10），两个操作同时发生。

事务 A：提交当前题目答案（更新已有记录的评分）
sql
UPDATE voice_interview_qa
SET score = 85
WHERE id = 1001 AND interview_id = 10;
✅ 事务 A 对 voice_interview_qa 的 id=1001 行加排他锁

✅ 事务 A 对 voice_interview 的 id=10 行加共享锁（验证外键 interview_id=10 是否存在）

此时事务 A 持有：

QA 表行锁（排他）

Interview 表共享锁

事务 B：更新父表状态（修改面试主表）
sql
UPDATE voice_interview
SET status = ''FINISHED''
WHERE id = 10;
事务 B 尝试对 voice_interview 的 id=10 行加排他锁

但该行已被事务 A 持有共享锁 → 阻塞等待

此时事务 B 持有的只有事务 B 自己做的修改的 UNDO 记录，以及等待事务 A 释放共享锁的状态。

（关键）事务 A 继续执行，尝试插入新的 QA 记录（下一题）
sql
INSERT INTO voice_interview_qa (interview_id, question_id, answer)
VALUES (10, 1002, ''...'');
在执行插入时，MySQL 需要在 voice_interview_qa 表上：

对 interview_id 加共享锁（或者叫“插入意图锁”）

更重要的是：MySQL 会检查 voice_interview 表上 id=10 这一行是否存在

这个检查需要访问 voice_interview 表，而事务 B 虽然在等待排他锁，但它已经在该行上标记了等待排他锁的意向——这个等待本身会产生一个锁等待队列的权重。MySQL 的锁调度器会认为事务 B 的排他锁请求优先级更高。

于是事务 A 在尝试访问父表 voice_interview 时，被事务 B 的等待给挡回来了。

关键点：事务 A 卡在哪把锁上？
事务 A 卡在“INSERT 语句自动获取父表共享锁”这一步。

它已经持有父表的共享锁，但为什么还会卡？因为事务 B 的排他锁请求已经在父表上排队了，共享锁与排他锁互斥，尽管事务 A 已经持有共享锁，在特定场景下（INSERT 需要获取“插入意图锁”时），MySQL 仍会优先让排他锁插队。

循环等待闭合：

事务 A 持有子表 qa 的行锁（id=1001），等待父表 interview 的插入意图锁释放

事务 B 等待父表 interview 的排他锁（被事务 A 阻塞）

事务 B 尚未释放任何锁，但它的锁请求已经让父表的锁队列产生了“排他优先”的规则

最终形成的闭环是：

text
事务 A：持有 QA 表行锁 → 等待 Interview 表插入意图锁（由事务 B 的排他锁请求阻挡）
事务 B：等待 Interview 表排他锁（被事务 A 持有的共享锁阻挡）
         ↑                                              ↓
         └────────────────── 循环等待 ──────────────────┘
为什么“先更新主表，再更新子表”能解决？
事务 B 先拿到 Interview 表排他锁（它先更新父表）

事务 B 完成更新后释放锁

事务 A 再更新子表时，父表已经没有锁竞争了

永远不会出现“一个先锁父表、另一个先锁子表”的交叉情况

一句话闭环
事务 A 等的是“事务 B 排他锁请求造成的父表锁队列阻塞”，最终表现为等父表插入意图锁；事务 B 等的是“事务 A 持有的父表共享锁释放”。两者互为因果，形成死锁。',0,'你能主动修正“锁升级”的说法，并尝试用事务闭环解释死锁，结构意识不错。问题是对InnoDB锁机制的关键细节不够准确，尤其是隐式锁、等待队列和S锁兼容性。','你能纠正锁升级这个点不错，但隐式锁和锁队列解释还不够准。',66,'{
  "relevance" : 80,
  "professionalism" : 58,
  "fluency" : 80,
  "logic" : 60,
  "confidence" : 78
}',NULL,'{
  "reply" : "你能纠正锁升级这个点不错，但隐式锁和锁队列解释还不够准。",
  "score" : 65,
  "dimensions" : {
    "relevance" : 80,
    "professionalism" : 58,
    "fluency" : 80,
    "logic" : 60,
    "confidence" : 78
  },
  "feedback" : "你能主动修正“锁升级”的说法，并尝试用事务闭环解释死锁，结构意识不错。问题是对InnoDB锁机制的关键细节不够准确，尤其是隐式锁、等待队列和S锁兼容性。",
  "flaws" : [ "未明确回答InnoDB是否支持锁升级，以及所谓升级对应哪条SQL。", "把事务B等待排他锁描述为“隐式锁”，混淆了锁持有与锁等待。", "事务A已持有父表S锁后再次请求S锁会被X等待阻塞的说法，缺少InnoDB锁兼容矩阵依据。" ],
  "redFlags" : [ "对“隐式锁”“插入意图锁”的使用与外键检查场景不完全匹配，疑似拼凑术语。", "修正后的死锁闭环仍与常见InnoDB锁行为存在不一致，需要进一步核验。" ],
  "sentiment" : {
    "state" : "confident",
    "note" : "使用“先说结论”“一句话闭环”等表达，语气较笃定。"
  },
  "fluencyAssessment" : {
    "score" : 80,
    "comment" : "整体表达连贯，有分步说明，少量口语化修正但不影响理解。"
  },
  "completeness" : {
    "covered" : [ "事务A和事务B的SQL及锁状态", "对“锁升级”说法的修正", "通过调整父子表操作顺序避免死锁的思路" ],
    "missing" : [ "InnoDB是否支持锁升级的明确结论", "原“升级锁”说法对应的具体SQL或明确否认", "基于死锁日志或锁兼容矩阵的验证依据" ]
  },
  "level" : "mid",
  "followupWorth" : true,
  "nextAction" : "deepen",
  "nextQuestion" : "你提到“事务 B 的排他锁请求已经在父表上排队了”，请结合InnoDB锁兼容矩阵说明：事务A已持有父表S锁后，再次请求S锁为什么会被等待X锁的事务阻塞？",
  "candidateId" : null,
  "transition" : "",
  "guidance" : ""
}',0,35445,'next','2026-09-07 16:50:00','2026-09-07 16:50:00','0'),
                                                                                                                                                                                                                                                                                                                         (4,NULL,'llm',1,NULL,'你提到“事务 B 的排他锁请求已经在父表上排队了”，请结合InnoDB锁兼容矩阵说明：事务A已持有父表S锁后，再次请求S锁为什么会被等待X锁的事务阻塞？','事务A被阻塞，不是因为S锁和X锁不兼容（它们当然不兼容），而是因为InnoDB的FIFO排队规则：一个已经持有S锁的事务，再次请求S锁（可能是插入意图锁）时，会被队列中更早的X锁请求“挡住”。这是为了避免“写饥饿”，确保等待中的X锁能尽早获得资源',0,'你能抓住等待队列和避免写饥饿这个核心，说明有并发调度意识。问题是对同一事务重复请求S锁、插入意向锁与普通记录锁的边界还不够确定。','可以，方向对，但要区分同一事务重复S锁和插入意向锁，不然解释不严谨。',68,'{
  "relevance" : 85,
  "professionalism" : 78,
  "fluency" : 80,
  "logic" : 72,
  "confidence" : 75
}',NULL,'{
  "reply" : "可以，方向对，但要区分同一事务重复S锁和插入意向锁，不然解释不严谨。",
  "score" : 72,
  "dimensions" : {
    "relevance" : 85,
    "professionalism" : 78,
    "fluency" : 80,
    "logic" : 72,
    "confidence" : 75
  },
  "feedback" : "你能抓住等待队列和避免写饥饿这个核心，说明有并发调度意识。问题是对同一事务重复请求S锁、插入意向锁与普通记录锁的边界还不够确定。",
  "flaws" : [ "把再次请求S锁被阻塞简单归因于FIFO，未区分同一事务重复持锁场景。", "提到“可能是插入意图锁”，说明对实际锁类型判断不够确定。", "未说明插入意向锁与普通S锁、记录锁在兼容矩阵中的具体差异。" ],
  "redFlags" : [ "“FIFO排队规则、写饥饿”表述略模板化，缺少与具体死锁日志或索引锁的对应验证。" ],
  "sentiment" : {
    "state" : "confident",
    "note" : "回答整体肯定，能用结论式语言解释机制，但个别措辞显露不确定性。"
  },
  "fluencyAssessment" : {
    "score" : 80,
    "comment" : "表达较连贯，逻辑推进清楚，但“可能是”这类措辞略影响笃定感。"
  },
  "completeness" : {
    "covered" : [ "等待中的X锁可能阻塞后续兼容锁请求", "避免写饥饿的排队机制", "锁队列对并发调度的影响" ],
    "missing" : [ "同一事务重复请求S锁是否真的会进入等待", "插入意向锁与普通S锁的兼容关系", "如何通过INNODB STATUS或锁表验证具体等待原因" ]
  },
  "level" : "mid",
  "followupWorth" : true,
  "nextAction" : "deepen",
  "nextQuestion" : "你刚说“可能是插入意图锁”，请说明插入意向锁和普通S锁在父表id=10这行上会不会互相阻塞？",
  "candidateId" : null,
  "transition" : "",
  "guidance" : "请区分同一事务重复请求S锁和插入意向锁这两种情况。"
}',0,44293,'followup','2026-09-07 16:52:01','2026-09-07 16:52:00','0'),
                                                                                                                                                                                                                                                                                                                         (4,NULL,'llm',1,12,'你刚说“可能是插入意图锁”，请说明插入意向锁和普通S锁在父表id=10这行上会不会互相阻塞？','会的
',0,'你能直接回应问题，但结论不准确。插入意向锁主要作用在索引间隙上，通常不会与普通记录S锁互斥。需要补充记录锁、间隙锁和Next-Key Lock的区别。','这个不太准，普通行S锁一般不会挡插入意向锁，除非是间隙或Next-Key。',27,'{
  "relevance" : 60,
  "professionalism" : 30,
  "fluency" : 55,
  "logic" : 25,
  "confidence" : 45
}',NULL,'{
  "reply" : "这个不太准，普通行S锁一般不会挡插入意向锁，除非是间隙或Next-Key。",
  "score" : 32,
  "dimensions" : {
    "relevance" : 60,
    "professionalism" : 30,
    "fluency" : 55,
    "logic" : 25,
    "confidence" : 45
  },
  "feedback" : "你能直接回应问题，但结论不准确。插入意向锁主要作用在索引间隙上，通常不会与普通记录S锁互斥。需要补充记录锁、间隙锁和Next-Key Lock的区别。",
  "flaws" : [ "将插入意向锁与普通记录S锁混为一谈。", "未说明插入意向锁作用对象是间隙而非已有行。", "未给出锁兼容矩阵或例外场景。" ],
  "redFlags" : [ ],
  "sentiment" : {
    "state" : "calm",
    "note" : "仅一个肯定词，未体现明显紧张或犹豫。"
  },
  "fluencyAssessment" : {
    "score" : 55,
    "comment" : "仅一个词，表达直接但缺乏展开，连贯性不足。"
  },
  "completeness" : {
    "covered" : [ "给出了是否阻塞的明确结论" ],
    "missing" : [ "插入意向锁的对象和语义", "与普通S锁的兼容关系", "Next-Key或间隙锁导致阻塞的例外", "与当前死锁场景的关系" ]
  },
  "level" : "junior",
  "followupWorth" : true,
  "nextAction" : "deepen",
  "nextQuestion" : "你刚说‘会的’，请具体说明插入意向锁是加在记录上还是间隙上，为什么会和普通S锁冲突？",
  "candidateId" : null,
  "transition" : "",
  "guidance" : ""
}',0,129579,'followup','2026-09-07 16:54:04','2026-09-07 16:54:03','0'),
                                                                                                                                                                                                                                                                                                                         (4,NULL,'llm',1,13,'你刚说‘会的’，请具体说明插入意向锁是加在记录上还是间隙上，为什么会和普通S锁冲突？','这个我不太记得了',0,'能坦诚说明不熟悉，避免硬编是加分项。但该问题正好暴露InnoDB锁细节掌握不足，尤其插入意向锁、间隙锁与记录锁的边界需要补齐。','没关系，可以记为：插入意向锁偏间隙锁，是否冲突要看S锁是否锁住间隙。',30,'{
  "relevance" : 25,
  "professionalism" : 55,
  "fluency" : 70,
  "logic" : 25,
  "confidence" : 35
}',NULL,'{
  "reply" : "没关系，可以记为：插入意向锁偏间隙锁，是否冲突要看S锁是否锁住间隙。",
  "score" : 35,
  "dimensions" : {
    "relevance" : 25,
    "professionalism" : 55,
    "fluency" : 70,
    "logic" : 25,
    "confidence" : 35
  },
  "feedback" : "能坦诚说明不熟悉，避免硬编是加分项。但该问题正好暴露InnoDB锁细节掌握不足，尤其插入意向锁、间隙锁与记录锁的边界需要补齐。",
  "flaws" : [ "未说明插入意向锁主要作用于索引间隙而非普通记录本身", "未区分普通记录S锁与间隙锁/Next-Key锁对插入意向锁的不同影响", "前一轮判断‘会的’缺少依据，说明概念仍不稳固" ],
  "redFlags" : [ "前一轮给出确定结论但本轮无法解释，可能存在记忆化表述或概念混淆" ],
  "sentiment" : {
    "state" : "hesitant",
    "note" : "直接表示不太记得，回答短且不确定"
  },
  "fluencyAssessment" : {
    "score" : 70,
    "comment" : "表达本身顺畅，但内容过短，没有技术展开"
  },
  "completeness" : {
    "covered" : [ "承认对该知识点不熟悉" ],
    "missing" : [ "插入意向锁是加在间隙上的特殊锁", "普通记录S锁通常不直接阻塞插入意向锁", "间隙锁或Next-Key锁会阻塞插入意向锁的原因" ]
  },
  "level" : "junior",
  "followupWorth" : false,
  "nextAction" : "change_topic",
  "nextQuestion" : "请说明HTTP与HTTPS的主要区别，以及HTTPS主要解决了什么问题？",
  "candidateId" : "3",
  "transition" : "这个点先不展开，我们换个方向，聊聊网络协议基础。",
  "guidance" : ""
}',0,223107,'next','2026-09-07 16:55:46','2026-09-07 16:55:46','0'),
                                                                                                                                                                                                                                                                                                                         (4,2,'bank',2,NULL,'请说明HTTP与HTTPS的主要区别，以及HTTPS主要解决了什么问题？','http明文传输，https是在http之上加上了加密和解密密文传输，通过私钥来解析，签名
',0,'回答一般，建议补充更多细节。关键词覆盖 2/4，答案长度 46 字。','你提到了明文和加密，但“通过私钥解析、签名”说法不太准确，HTTPS核心是SSL/TLS，能具体说说加密机制和证书作用吗？',45,'{
  "relevance" : 50,
  "professionalism" : 70,
  "fluency" : 23,
  "interactivity" : 65,
  "confidence" : 54,
  "logic" : 75
}',NULL,'{
  "reply" : "你提到了明文和加密，但“通过私钥解析、签名”说法不太准确，HTTPS核心是SSL/TLS，能具体说说加密机制和证书作用吗？",
  "score" : null,
  "dimensions" : { },
  "feedback" : "",
  "flaws" : [ ],
  "redFlags" : [ ],
  "sentiment" : null,
  "fluencyAssessment" : null,
  "completeness" : null,
  "level" : "",
  "followupWorth" : false,
  "nextAction" : "",
  "nextQuestion" : "",
  "candidateId" : null,
  "transition" : "",
  "guidance" : ""
}',0,52567,'next','2026-09-07 16:56:54','2026-09-07 16:56:53','0'),
                                                                                                                                                                                                                                                                                                                         (4,4,'bank',3,NULL,'以下哪种情况会产生死锁？',NULL,0,NULL,'好，我们换个话题。 以下哪种情况会产生死锁？',NULL,NULL,NULL,NULL,1,NULL,NULL,'2026-09-07 16:58:52','2026-09-07 16:58:52','0'),
                                                                                                                                                                                                                                                                                                                         (5,NULL,'self_intro',-1,NULL,'请先做一个约3分钟的自我介绍，内容包括你的基本情况、技术栈、项目经历和求职方向。','你好我是钟永国
',0,'自我介绍偏短，建议补充技术栈、项目亮点与求职方向。','自我介绍偏短，建议补充技术栈、项目亮点与求职方向。',46,'{
  "structure" : 40,
  "awareness" : 45,
  "matching" : 55,
  "fluency" : 45
}',NULL,NULL,0,NULL,'next','2026-09-11 16:23:35','2026-09-11 16:23:34','0'),
                                                                                                                                                                                                                                                                                                                         (5,1,'bank',0,NULL,'TCP三次握手的目的是什么？','好的，最近在做自己项目，旭林知行
',0,'能主动谈及个人项目值得肯定，但当前问题是TCP三次握手的目的，你的回答完全未涉及任何技术要点。','我理解你想分享项目，但这个问题是TCP三次握手的目的，你还没回答到点上，先回到技术问题。',15,'{
  "relevance" : 5,
  "professionalism" : 10,
  "fluency" : 40,
  "logic" : 10,
  "confidence" : 20
}',NULL,'{
  "reply" : "我理解你想分享项目，但这个问题是TCP三次握手的目的，你还没回答到点上，先回到技术问题。",
  "score" : 20,
  "dimensions" : {
    "relevance" : 5,
    "professionalism" : 10,
    "fluency" : 40,
    "logic" : 10,
    "confidence" : 20
  },
  "feedback" : "能主动谈及个人项目值得肯定，但当前问题是TCP三次握手的目的，你的回答完全未涉及任何技术要点。",
  "flaws" : [ "回答内容为个人项目介绍，与TCP三次握手问题完全无关", "未提及任何关于序列号、连接建立或可靠传输的技术概念", "对基础网络问题缺乏回应，可能暴露知识盲区或未听清题目" ],
  "redFlags" : [ "答非所问，疑似未听清或刻意回避技术问题" ],
  "sentiment" : {
    "state" : "hesitant",
    "note" : "回答简短且转向项目，显得迟疑回避。"
  },
  "fluencyAssessment" : {
    "score" : 40,
    "comment" : "回答仅有一句且含语气词，虽无明显重复但没有任何实质内容。"
  },
  "completeness" : {
    "covered" : [ ],
    "missing" : [ "三次握手的目的", "同步初始序列号ISN", "保证数据传输有序可靠" ]
  },
  "level" : "junior",
  "followupWorth" : true,
  "nextAction" : "deepen",
  "nextQuestion" : "你刚提到“旭林知行”，这个和TCP三次握手有直接关系吗？如果没有，请先回答：TCP三次握手最核心的目的是什么？",
  "candidateId" : null,
  "transition" : "",
  "guidance" : "我们先把项目放一边，直接回答TCP三次握手的目的。"
}',0,37635,'followup','2026-09-11 16:24:10','2026-09-11 16:24:10','0'),
                                                                                                                                                                                                                                                                                                                         (5,1,'llm',0,18,'你刚提到“旭林知行”，这个和TCP三次握手有直接关系吗？如果没有，请先回答：TCP三次握手最核心的目的是什么？','好的，这是是一个主要是为面试者提供一个模拟真实面试场景的平台，主要有后端服务，后台管理平台，前台门户网站三个部分组成，项目功能包括了简历分析，优化，岗位维护，以及ai语音模拟面试，以及题库，在线做题，在线编程等方面的技术刷题功能，三个大功能相辅相成，基于自研的AI 能力底座，实现AI简历分析，AI 简历优化，AI出题，AI实时语音交互面试等等功能，实现一个集学习，简历，面试于一体的网站，此外网站还接入社区创作，文章发布和阅览功能模块，读书空间，书籍在线阅读，社区互动等功能，每个模块都有对应的后台管理系统进行维护，这个平台主要是面向广大求职者，为他们找工作和学习成长提供帮助，助力人们找到满意哦工作；
',0,'你介绍项目时表达较流畅，但这次回答完全没涉及TCP三次握手的目的，属于答非所问。','你刚才说的是项目介绍，但我问的是TCP三次握手的目的。先回到问题本身，可以吗？',17,'{
  "relevance" : 5,
  "professionalism" : 20,
  "fluency" : 60,
  "logic" : 45,
  "confidence" : 55
}',NULL,'{
  "reply" : "你刚才说的是项目介绍，但我问的是TCP三次握手的目的。先回到问题本身，可以吗？",
  "score" : 15,
  "dimensions" : {
    "relevance" : 5,
    "professionalism" : 20,
    "fluency" : 60,
    "logic" : 45,
    "confidence" : 55
  },
  "feedback" : "你介绍项目时表达较流畅，但这次回答完全没涉及TCP三次握手的目的，属于答非所问。",
  "flaws" : [ "回答内容为个人项目介绍，与TCP三次握手问题完全无关。", "疑似背诵事先准备好的项目材料，未理解面试官提问。", "未展示任何TCP协议相关基础知识。" ],
  "redFlags" : [ "答非所问：候选人在被问基础网络问题时抛出一段项目背景，疑似未认真审题或背诵。", "背诵痕迹：大段连贯的项目描述具有明显准备痕迹，缺少临场组织。" ],
  "sentiment" : {
    "state" : "nervous",
    "note" : "开头出现''这是是一个''重复，随后转为流利背诵，可能因紧张导致跑题。"
  },
  "fluencyAssessment" : {
    "score" : 60,
    "comment" : "开头有''这是是一个''口误和重复，后续流利但像是背稿，停顿极少。"
  },
  "completeness" : {
    "covered" : [ ],
    "missing" : [ "TCP三次握手目的：同步初始序列号（ISN），确保可靠传输", "首次握手SYN、第二次SYN+ACK、第三次ACK的基本流程", "防止旧的重复连接初始化造成混乱" ]
  },
  "level" : "junior",
  "followupWorth" : false,
  "nextAction" : "change_topic",
  "nextQuestion" : "进程和线程的本质区别是什么？",
  "candidateId" : "4",
  "transition" : "那我们先换一道题，考察操作系统基础，请直接回答。",
  "guidance" : "请针对TCP三次握手的目的进行回答，刚才的内容是项目介绍，不是本题要求。"
}',0,74415,'next','2026-09-11 16:24:53','2026-09-11 16:24:52','0'),
                                                                                                                                                                                                                                                                                                                         (5,3,'bank',1,NULL,'进程和线程的本质区别是什么？','不会
',0,'能坦诚说不会也算真诚，但操作系统最基础的概念需要尽快补上。','没关系，基础概念后面可以补，我们先换一题。',7,'{
  "relevance" : 0,
  "professionalism" : 0,
  "fluency" : 50,
  "logic" : 0,
  "confidence" : 20
}',NULL,'{
  "reply" : "没关系，基础概念后面可以补，我们先换一题。",
  "score" : 10,
  "dimensions" : {
    "relevance" : 0,
    "professionalism" : 0,
    "fluency" : 50,
    "logic" : 0,
    "confidence" : 20
  },
  "feedback" : "能坦诚说不会也算真诚，但操作系统最基础的概念需要尽快补上。",
  "flaws" : [ "无法回答进程与线程的本质区别，暴露操作系统基础概念缺失" ],
  "redFlags" : [ ],
  "sentiment" : {
    "state" : "nervous",
    "note" : "直接回答“不会”，语气简短，可能因紧张或准备不足"
  },
  "fluencyAssessment" : {
    "score" : 50,
    "comment" : "回答简短无口头禅，但无实质内容，无法判断语言组织连贯性"
  },
  "completeness" : {
    "covered" : [ ],
    "missing" : [ "进程是资源分配的最小单位", "线程是CPU调度的最小单位", "线程存在于进程内，共享进程地址空间", "线程间通信成本低于进程间通信", "线程需要同步机制保证数据一致性" ]
  },
  "level" : "junior",
  "followupWorth" : false,
  "nextAction" : "change_topic",
  "nextQuestion" : "HTTP与HTTPS的主要区别是什么？",
  "candidateId" : "5",
  "transition" : "没关系，我们换个方向，看看网络基础。请说说HTTP与HTTPS的主要区别。",
  "guidance" : ""
}',0,122187,'next','2026-09-11 16:25:35','2026-09-11 16:25:35','0');
INSERT INTO `moyun-db`.portal_voice_interview_qa (interview_id,question_id,question_source,question_idx,parent_qa_id,question,user_answer,transcription_edited,ai_feedback,speak_text,score,rule_dimensions_json,llm_score_json,llm_analysis_json,hint_used,latency_ms,next_action,create_time,update_time,del_flag) VALUES
                                                                                                                                                                                                                                                                                                                         (5,2,'bank',2,NULL,'HTTP与HTTPS的主要区别是什么？','你反复问了，
',0,'能感觉到你有些情绪，沟通态度需要调整；这次没有回答HTTP和HTTPS的区别，基础网络知识需要复习。','没关系，那我们就到这里，感谢你今天的时间。',14,'{
  "relevance" : 10,
  "professionalism" : 20,
  "fluency" : 30,
  "logic" : 15,
  "confidence" : 25
}',NULL,'{
  "reply" : "没关系，那我们就到这里，感谢你今天的时间。",
  "score" : 20,
  "dimensions" : {
    "relevance" : 10,
    "professionalism" : 20,
    "fluency" : 30,
    "logic" : 15,
    "confidence" : 25
  },
  "feedback" : "能感觉到你有些情绪，沟通态度需要调整；这次没有回答HTTP和HTTPS的区别，基础网络知识需要复习。",
  "flaws" : [ "未正面回答HTTP与HTTPS的区别，回答与问题无关", "使用了''你反复问了''这种带情绪的表达，缺乏专业沟通", "没有提及加密、SSL/TLS、端口等任何关键知识点" ],
  "redFlags" : [ "答非所问，且带有抵触情绪", "回答内容为零，疑似放弃或不愿继续" ],
  "sentiment" : {
    "state" : "nervous",
    "note" : "语气中透露出不耐烦，可能因为之前问题受挫而焦虑"
  },
  "fluencyAssessment" : {
    "score" : 40,
    "comment" : "虽然回答只有一句且连贯，但内容完全偏离，没有信息量"
  },
  "completeness" : {
    "covered" : [ ],
    "missing" : [ "HTTP默认端口80", "HTTPS默认端口443", "SSL/TLS加密层", "需要CA证书", "数据机密性和完整性" ]
  },
  "level" : "junior",
  "followupWorth" : false,
  "nextAction" : "wrap_up",
  "nextQuestion" : "好的，今天的面试就到这里，感谢你的参与，后续有通知我们会联系你。",
  "candidateId" : null,
  "transition" : "今天的面试环节已经完成，感谢你的时间和配合。",
  "guidance" : "如果后面复习，建议重点记忆HTTPS比HTTP多出的SSL/TLS加密层和默认端口443。"
}',0,162755,'next','2026-09-11 16:26:12','2026-09-11 16:26:12','0'),
                                                                                                                                                                                                                                                                                                                         (5,NULL,'candidate_ask',2,NULL,'好的，我这边的问题问完了。你有什么想问我的吗？关于岗位、团队或技术方向都可以。',NULL,0,NULL,'好的，我这边的问题问完了。你有什么想问我的吗？',NULL,NULL,NULL,NULL,0,NULL,NULL,'2026-09-11 16:27:06','2026-09-11 16:27:06','0');
INSERT INTO `moyun-db`.portal_writing_prompt (prompt_date,title,description,category,festival_name,source,created_time,del_flag) VALUES
                                                                                                                                     ('2026-08-28','处暑后的周五，收起短袖','傍晚的风终于带凉，衣柜该换季了。可以写你整理一件旧短袖时闻到的洗衣液味、摸到口袋里的票根，或突然想起的那个夏天。','生活',NULL,'ai','2026-08-28 15:47:53','0'),
                                                                                                                                     ('2026-08-29','处暑过后，傍晚的风开始凉了','写作切入点：写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。','生活',NULL,'ai','2026-08-28 15:48:33','0');
INSERT INTO `moyun-db`.portal_wrong_question (user_id,question_id,attempt_id,status,wrong_count,last_wrong_time,next_review_time,created_time,del_flag) VALUES
                                                                                                                                                            (6,7,1,'mastered',1,'2026-08-31 10:55:51','2026-09-01 10:55:51','2026-08-31 10:55:51','0'),
                                                                                                                                                            (6,1,18,'wrong',2,'2026-09-11 16:27:06','2026-09-13 16:27:06','2026-09-07 16:35:06','0'),
                                                                                                                                                            (6,3,20,'wrong',2,'2026-09-11 16:27:06','2026-09-13 16:27:06','2026-09-07 16:35:06','0'),
                                                                                                                                                            (6,2,21,'wrong',1,'2026-09-11 16:27:06','2026-09-12 16:27:06','2026-09-11 16:27:06','0');
INSERT INTO `moyun-db`.sys_audit_task (task_type,biz_type,biz_id,title,description,submitter_id,submitter_name,status,auditor_id,auditor_name,audit_opinion,audit_action,submit_time,audit_time,priority,route_path,extra_data,create_time,update_time) VALUES
                                                                                                                                                                                                                                                            ('article',NULL,1,'22222222222222222222222222','222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222',6,'zhangsan','approved',1,'admin','11','approve','2026-08-28 09:58:10','2026-08-28 09:58:30','medium','/portal/audit-center',NULL,'2026-08-28 09:58:10','2026-08-28 09:58:30'),
                                                                                                                                                                                                                                                            ('certification',NULL,1,'创作者认证申请-李白','',7,'libai','approved',1,'admin','','approve','2026-08-28 10:03:46','2026-08-28 10:04:19','medium','/portal/audit-center',NULL,'2026-08-28 10:03:46','2026-08-28 10:04:19'),
                                                                                                                                                                                                                                                            ('article',NULL,13,'李白文章解析','李白（701年—762年），字太白，号青莲居士，被后世尊称为“诗仙”。他不仅是唐代伟大的浪漫主义诗人，更是一位具有宏大政治抱负和深邃思想体系的思想家。他的文章与诗歌，既是个人情感的宣泄，也是盛唐时代精神的缩影。 一、 思想内核：融汇百川的“思想家” 李白并非单纯的隐士或狂客，他构建了一个以“建功济世”为主导，以“安社稷',7,'libai','approved',1,'admin','上帝发誓是 ','approve','2026-08-28 13:12:23','2026-08-28 17:55:00','medium','/portal/audit-center',NULL,'2026-08-28 13:12:23','2026-08-28 17:55:00'),
                                                                                                                                                                                                                                                            ('article',NULL,7,'web 端如何实现一个语音转文字的','Web 端实现语音转文字，主要有以下几种方案，从简单到复杂依次介绍：方案一：Web Speech API（最简单，推荐先用这个）这是浏览器原生提供的能力，零依赖、无需后端，几行代码就能跑起来。完整示例代码html预览<!',NULL,NULL,'approved',1,'admin','全额付清我','approve','2026-08-28 13:12:39','2026-08-28 17:54:45','medium','/portal/audit-center',NULL,'2026-08-28 13:12:39','2026-08-28 17:54:45'),
                                                                                                                                                                                                                                                            ('article',NULL,16,'32242432','本文围绕标题“32242432”与正文“斯巴达”等有限信息，生成内容摘要与SEO优化信息，适合测试或占位页面使用。',6,'zhangsan','approved',1,'admin','撒旦v撒','approve','2026-08-28 17:50:27','2026-08-28 17:58:00','medium','/portal/audit-center',NULL,'2026-08-28 17:50:27','2026-08-28 17:58:00'),
                                                                                                                                                                                                                                                            ('interview_exp',NULL,1,'java面试','面试',6,'zhangsan','approved',1,'admin','好的','approve','2026-08-31 10:42:07','2026-08-31 10:42:47','medium','/portal/audit-center',NULL,'2026-08-31 10:42:07','2026-08-31 10:42:47'),
                                                                                                                                                                                                                                                            ('article',NULL,14,'22222222','22222222',NULL,NULL,'approved',1,'admin','','approve','2026-08-31 16:22:50','2026-08-31 16:23:19','medium','/portal/audit-center',NULL,'2026-08-31 16:22:50','2026-08-31 16:23:19'),
                                                                                                                                                                                                                                                            ('article',NULL,17,'处暑过后，傍晚的风开始凉了','写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。',6,'zhangsan','approved',1,'admin','','approve','2026-09-07 10:03:07','2026-09-07 10:05:37','medium','/portal/audit-center',NULL,'2026-09-07 10:03:07','2026-09-07 10:05:37'),
                                                                                                                                                                                                                                                            ('article',NULL,18,'手机端发起的第一个文章','<p>http://localhost:3000/前台首页需要做一次ui布局样式等重构，请根据原型html 格局语义修改布局，其中包括头部，中部，尾部，因为重构的原型可能缺少一些元素，在改造过程中，缺失的内容需要补上，放在合理位置，尤其是头部的 帮助中心，主题切换，搜索，消息等小的按钮，原型在moyun-project-document\\docs\\08-原型设计目录下，其中有两个文件，一个V1是用...',6,'zhangsan','approved',1,'admin','','approve','2026-09-07 10:15:28','2026-09-07 10:15:49','medium','/portal/audit-center',NULL,'2026-09-07 10:15:28','2026-09-07 10:15:49'),
                                                                                                                                                                                                                                                            ('topic',NULL,1,'哈哈哈，世界太变态了？','世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？',6,'zhangsan','approved',1,'admin','','approve','2026-09-07 17:52:17','2026-09-07 17:52:45','medium','/portal/audit-center',NULL,'2026-09-07 17:52:17','2026-09-07 17:52:45');
INSERT INTO `moyun-db`.sys_audit_task (task_type,biz_type,biz_id,title,description,submitter_id,submitter_name,status,auditor_id,auditor_name,audit_opinion,audit_action,submit_time,audit_time,priority,route_path,extra_data,create_time,update_time) VALUES
                                                                                                                                                                                                                                                            ('feedback','suggestion',1,'连注册都没有','没有注册功能',6,'zhangsan','approved',1,'admin','好的，我处理','approve','2026-09-08 14:23:49','2026-09-08 14:34:40','medium','/portal/audit-center',NULL,'2026-09-08 14:23:49','2026-09-08 14:34:40'),
                                                                                                                                                                                                                                                            ('feedback','suggestion',2,'解决一个问题吧','大v阿萨俺的大俺的大啊',6,'zhangsan','pending',NULL,NULL,NULL,NULL,'2026-09-08 17:36:32',NULL,'medium','/portal/audit-center',NULL,'2026-09-08 17:36:32','2026-09-08 17:36:32');
INSERT INTO `moyun-db`.sys_config (config_name,config_key,config_value,config_type,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                    ('主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue','Y','admin','2026-08-19 18:01:44','',NULL,'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow','0'),
                                                                                                                                                    ('用户管理-账号初始密码','sys.user.initPassword','123456','Y','admin','2026-08-19 18:01:44','',NULL,'初始化密码 123456','0'),
                                                                                                                                                    ('主框架页-侧边栏主题','sys.index.sideTheme','theme-dark','Y','admin','2026-08-19 18:01:44','',NULL,'深色主题theme-dark，浅色主题theme-light','0'),
                                                                                                                                                    ('账号自助-验证码开关','sys.account.captchaEnabled','true','Y','admin','2026-08-19 18:01:44','',NULL,'是否开启验证码功能（true开启，false关闭）','0'),
                                                                                                                                                    ('账号自助-是否开启用户注册功能','sys.account.registerUser','false','Y','admin','2026-08-19 18:01:44','',NULL,'是否开启注册用户功能（true开启，false关闭）','0'),
                                                                                                                                                    ('用户登录-黑名单列表','sys.login.blackIPList','','Y','admin','2026-08-19 18:01:44','',NULL,'设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）','0'),
                                                                                                                                                    ('平台服务费率','pay.platform.fee-rate','0.10','Y','admin','2026-08-28 09:24:22','',NULL,'V11.0 支付分账：平台抽成比例（0.10=10%），运行时生效','0'),
                                                                                                                                                    ('语音面试默认面试官AgentID','voice.interview.defaultAgentId','48','Y','admin','2026-09-07 09:15:33','',NULL,'语音面试绑定的 ai_agent 主键；编辑「AI模块→智能体管理」对应 agent 的人设/提示词/模型即动态生效','0'),
                                                                                                                                                    ('语音面试动态出题开关','voice.interview.dynamicMode','true','Y','admin','2026-09-07 09:15:33','',NULL,'true=智能体动态出题（结合上下文随机应变），false=沿用预生成题单（旧行为）','0');
INSERT INTO `moyun-db`.sys_dept (parent_id,ancestors,dept_name,order_num,leader,phone,email,status,del_flag,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                    (0,'0','若依科技',0,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (100,'0,100','深圳总公司',1,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (100,'0,100','长沙分公司',2,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (101,'0,100,101','研发部门',1,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (101,'0,100,101','市场部门',2,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (101,'0,100,101','测试部门',3,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (101,'0,100,101','财务部门',4,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (101,'0,100,101','运维部门',5,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (102,'0,100,102','市场部门',1,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                                                                                    (102,'0,100,102','财务部门',2,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL);
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                        (1,'男','0','sys_user_sex','','','Y','0','admin','2026-08-19 18:01:44','',NULL,'性别男','0'),
                                                                                                                                                                                        (2,'女','1','sys_user_sex','','','N','0','admin','2026-08-19 18:01:44','',NULL,'性别女','0'),
                                                                                                                                                                                        (3,'未知','2','sys_user_sex','','','N','0','admin','2026-08-19 18:01:44','',NULL,'性别未知','0'),
                                                                                                                                                                                        (1,'显示','0','sys_show_hide','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'显示菜单','0'),
                                                                                                                                                                                        (2,'隐藏','1','sys_show_hide','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'隐藏菜单','0'),
                                                                                                                                                                                        (1,'正常','0','sys_normal_disable','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'正常状态','0'),
                                                                                                                                                                                        (2,'停用','1','sys_normal_disable','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'停用状态','0'),
                                                                                                                                                                                        (1,'正常','0','sys_job_status','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'正常状态','0'),
                                                                                                                                                                                        (2,'暂停','1','sys_job_status','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'停用状态','0'),
                                                                                                                                                                                        (1,'默认','DEFAULT','sys_job_group','','','Y','0','admin','2026-08-19 18:01:44','',NULL,'默认分组','0');
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                        (2,'系统','SYSTEM','sys_job_group','','','N','0','admin','2026-08-19 18:01:44','',NULL,'系统分组','0'),
                                                                                                                                                                                        (1,'是','Y','sys_yes_no','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'系统默认是','0'),
                                                                                                                                                                                        (2,'否','N','sys_yes_no','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'系统默认否','0'),
                                                                                                                                                                                        (1,'通知','1','sys_notice_type','','warning','Y','0','admin','2026-08-19 18:01:44','',NULL,'通知','0'),
                                                                                                                                                                                        (2,'公告','2','sys_notice_type','','success','N','0','admin','2026-08-19 18:01:44','',NULL,'公告','0'),
                                                                                                                                                                                        (1,'正常','0','sys_notice_status','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'正常状态','0'),
                                                                                                                                                                                        (2,'关闭','1','sys_notice_status','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'关闭状态','0'),
                                                                                                                                                                                        (99,'其他','0','sys_oper_type','','info','N','0','admin','2026-08-19 18:01:44','',NULL,'其他操作','0'),
                                                                                                                                                                                        (1,'新增','1','sys_oper_type','','info','N','0','admin','2026-08-19 18:01:44','',NULL,'新增操作','0'),
                                                                                                                                                                                        (2,'修改','2','sys_oper_type','','info','N','0','admin','2026-08-19 18:01:44','',NULL,'修改操作','0');
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                        (3,'删除','3','sys_oper_type','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'删除操作','0'),
                                                                                                                                                                                        (4,'授权','4','sys_oper_type','','primary','N','0','admin','2026-08-19 18:01:44','',NULL,'授权操作','0'),
                                                                                                                                                                                        (5,'导出','5','sys_oper_type','','warning','N','0','admin','2026-08-19 18:01:44','',NULL,'导出操作','0'),
                                                                                                                                                                                        (6,'导入','6','sys_oper_type','','warning','N','0','admin','2026-08-19 18:01:44','',NULL,'导入操作','0'),
                                                                                                                                                                                        (7,'强退','7','sys_oper_type','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'强退操作','0'),
                                                                                                                                                                                        (8,'生成代码','8','sys_oper_type','','warning','N','0','admin','2026-08-19 18:01:44','',NULL,'生成操作','0'),
                                                                                                                                                                                        (9,'清空数据','9','sys_oper_type','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'清空操作','0'),
                                                                                                                                                                                        (1,'成功','0','sys_common_status','','primary','N','0','admin','2026-08-19 18:01:44','',NULL,'正常状态','0'),
                                                                                                                                                                                        (2,'失败','1','sys_common_status','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'停用状态','0'),
                                                                                                                                                                                        (1,'待支付','pending','portal_pay_status','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                        (2,'已支付','paid','portal_pay_status','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (3,'已退款','refunded','portal_pay_status','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (4,'已关闭','closed','portal_pay_status','','danger','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (5,'支付失败','failed','portal_pay_status','','danger','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (1,'积分','points','portal_pay_channel','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (2,'支付宝','alipay','portal_pay_channel','','primary','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (3,'微信','wechat','portal_pay_channel','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (4,'钱包','wallet','portal_pay_channel','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (1,'待开始','idle','voice_interview_status','','info','Y','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (2,'聆听中','listening','voice_interview_status','','primary','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                        (3,'播报中','speaking','voice_interview_status','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (4,'评分中','scoring','voice_interview_status','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (5,'已结束','done','voice_interview_status','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (1,'专业','professional','voice_interview_style','','primary','Y','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (2,'亲和','friendly','voice_interview_style','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (3,'严格','strict','voice_interview_style','','danger','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (1,'切入点提示','1','voice_interview_hint_level','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (2,'结构提示','2','voice_interview_hint_level','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (3,'全量提示','3','voice_interview_hint_level','','danger','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (1,'技术岗','tech','portal_resume_category','','primary','Y','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                        (2,'产品岗','product','portal_resume_category','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (3,'应届生','fresh','portal_resume_category','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (4,'社招','social','portal_resume_category','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (5,'实习','intern','portal_resume_category','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                        (1,'展示阅读','reading','portal_practice_mode','','default','Y','0','admin','2026-08-20 10:56:17','',NULL,'看题+答案','0'),
                                                                                                                                                                                        (2,'选择题','choice','portal_practice_mode','','success','N','0','admin','2026-08-20 10:56:17','',NULL,'选项作答+判分','0'),
                                                                                                                                                                                        (3,'编程题','coding','portal_practice_mode','','primary','N','0','admin','2026-08-20 10:56:17','',NULL,'代码作答+测试用例判定','0'),
                                                                                                                                                                                        (1,'随时到岗','随时到岗','portal_available_time','','primary','Y','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
                                                                                                                                                                                        (2,'一周内到岗','一周内到岗','portal_available_time','','success','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
                                                                                                                                                                                        (3,'两周内到岗','两周内到岗','portal_available_time','','info','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                        (4,'一个月内到岗','一个月内到岗','portal_available_time','','warning','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
                                                                                                                                                                                        (5,'三个月内到岗','三个月内到岗','portal_available_time','','info','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
                                                                                                                                                                                        (6,'面议','面议','portal_available_time','','default','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
                                                                                                                                                                                        (1,'学生','student','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'学生群体','0'),
                                                                                                                                                                                        (2,'上班族','office_worker','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'固定职业收入','0'),
                                                                                                                                                                                        (3,'自由职业','freelancer','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'非固定收入','0'),
                                                                                                                                                                                        (4,'个体经营者','business_owner','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'经营性收入','0'),
                                                                                                                                                                                        (5,'退休','retired','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'退休群体','0'),
                                                                                                                                                                                        (6,'其他','other','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'其他身份','0'),
                                                                                                                                                                                        (1,'首页-旭林广告位','home_xulin_ad','portal_ad_slot_key','','success','N','0','admin','2026-08-28 13:16:38','',NULL,'首页-热门推荐上方的旭林广告位','0');
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
    (2,'首页-VIP推广位','home_vip_banner','portal_ad_slot_key','','primary','N','0','admin','2026-08-28 13:16:38','',NULL,'首页右侧/移动端下方的VIP推广位','0');
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                  ('用户性别','sys_user_sex','0','admin','2026-08-19 18:01:44','',NULL,'用户性别列表','0'),
                                                                                                                                  ('菜单状态','sys_show_hide','0','admin','2026-08-19 18:01:44','',NULL,'菜单状态列表','0'),
                                                                                                                                  ('系统开关','sys_normal_disable','0','admin','2026-08-19 18:01:44','',NULL,'系统开关列表','0'),
                                                                                                                                  ('任务状态','sys_job_status','0','admin','2026-08-19 18:01:44','',NULL,'任务状态列表','0'),
                                                                                                                                  ('任务分组','sys_job_group','0','admin','2026-08-19 18:01:44','',NULL,'任务分组列表','0'),
                                                                                                                                  ('系统是否','sys_yes_no','0','admin','2026-08-19 18:01:44','',NULL,'系统是否列表','0'),
                                                                                                                                  ('通知类型','sys_notice_type','0','admin','2026-08-19 18:01:44','',NULL,'通知类型列表','0'),
                                                                                                                                  ('通知状态','sys_notice_status','0','admin','2026-08-19 18:01:44','',NULL,'通知状态列表','0'),
                                                                                                                                  ('操作类型','sys_oper_type','0','admin','2026-08-19 18:01:44','',NULL,'操作类型列表','0'),
                                                                                                                                  ('系统状态','sys_common_status','0','admin','2026-08-19 18:01:44','',NULL,'登录状态列表','0');
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                  ('支付状态','portal_pay_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 支付状态','0'),
                                                                                                                                  ('支付渠道','portal_pay_channel','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 支付渠道','0'),
                                                                                                                                  ('打赏目标类型','portal_tip_target_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 打赏目标类型','0'),
                                                                                                                                  ('钱包交易类型','portal_wallet_txn_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 钱包交易类型','0'),
                                                                                                                                  ('文章状态','cms_article_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 文章状态','0'),
                                                                                                                                  ('专栏状态','cms_column_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 专栏状态','0'),
                                                                                                                                  ('话题状态','cms_topic_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 话题状态','0'),
                                                                                                                                  ('征文活动状态','cms_contest_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 征文活动状态','0'),
                                                                                                                                  ('审核任务类型','cms_audit_task_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 审核任务类型','0'),
                                                                                                                                  ('审核任务状态','cms_audit_task_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 审核任务状态','0');
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                  ('反馈类型','cms_feedback_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 反馈类型','0'),
                                                                                                                                  ('举报类型','cms_report_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 举报类型','0'),
                                                                                                                                  ('处理状态','cms_handle_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 处理状态','0'),
                                                                                                                                  ('书籍类型','portal_book_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 书籍类型','0'),
                                                                                                                                  ('书籍连载状态','portal_book_serial_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 书籍连载状态','0'),
                                                                                                                                  ('访问级别','portal_access_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 访问级别','0'),
                                                                                                                                  ('业务通用状态','portal_common_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 业务通用状态','0'),
                                                                                                                                  ('学习计划类型','portal_study_plan_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 学习计划类型','0'),
                                                                                                                                  ('学习计划状态','portal_study_plan_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 学习计划状态','0'),
                                                                                                                                  ('错题状态','portal_wrong_question_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 错题状态','0');
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                  ('题目难度','portal_question_difficulty','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 题目难度','0'),
                                                                                                                                  ('题目类型','portal_question_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 题目类型','0'),
                                                                                                                                  ('简历模板分类','portal_resume_category','0','admin','2026-08-19 18:01:46','',NULL,'v10.2 简历模板分类（英文值）','0'),
                                                                                                                                  ('广告位标识','portal_ad_slot_key','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 广告位标识','0'),
                                                                                                                                  ('VIP套餐状态','cms_vip_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 VIP套餐状态','0'),
                                                                                                                                  ('登录端类型','sys_login_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 登录端类型','0'),
                                                                                                                                  ('语音面试状态','voice_interview_status','0','admin','2026-08-19 18:01:46','',NULL,'v10.1 语音面试状态','0'),
                                                                                                                                  ('面试官风格','voice_interview_style','0','admin','2026-08-19 18:01:46','',NULL,'v10.1 面试官风格','0'),
                                                                                                                                  ('提示级别','voice_interview_hint_level','0','admin','2026-08-19 18:01:46','',NULL,'v10.1 提示级别','0'),
                                                                                                                                  ('练习模式','portal_practice_mode','0','admin','2026-08-20 10:56:17','',NULL,'题目练习模式：reading/choice/coding','0');
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                  ('简历到岗时间','portal_available_time','0','admin','2026-08-25 00:00:00','',NULL,'v10.10 简历求职意向-到岗时间（值为中文文本，直接入库）','0'),
                                                                                                                                  ('记账-身份标签','ledger_identity_tag','0','admin','2026-09-04 11:03:26','',NULL,'AI 财务分析用户画像身份标签','0');
INSERT INTO `moyun-db`.sys_file (file_name,file_ext,file_type,file_size,file_url,file_path,storage_type,bucket_name,object_name,fallback,local_path,file_md5,upload_user_id,upload_user_name,status,business_type,business_id,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                                                                               ('可爱.jpg','jpg','image',36835,'http://127.0.0.1:9001/moyun/2026/08/28/0d0edb42422c464cbfd548fcbe470efb.jpg','http://127.0.0.1:9001/moyun/2026/08/28/0d0edb42422c464cbfd548fcbe470efb.jpg','minio','moyun','2026/08/28/0d0edb42422c464cbfd548fcbe470efb.jpg',0,NULL,'b39afe6c9cadbe4708327429b847b1f9',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('可爱.jpg','jpg','image',36835,'http://127.0.0.1:9001/moyun/2026/08/28/7eaf5ae9c5034a819ba7e86dd32d4061.jpg','http://127.0.0.1:9001/moyun/2026/08/28/7eaf5ae9c5034a819ba7e86dd32d4061.jpg','minio','moyun','2026/08/28/7eaf5ae9c5034a819ba7e86dd32d4061.jpg',0,NULL,'b39afe6c9cadbe4708327429b847b1f9',7,'libai','0','creator_certification',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('熊大.png','png','image',27324,'http://127.0.0.1:9001/moyun/2026/08/28/10ada7c421034447a681e632f2f164a1.png','http://127.0.0.1:9001/moyun/2026/08/28/10ada7c421034447a681e632f2f164a1.png','minio','moyun','2026/08/28/10ada7c421034447a681e632f2f164a1.png',0,NULL,'b78ca092240c0631d496f75cf362d268',7,'libai','0','creator_certification',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('可爱.jpg','jpg','image',36835,'http://127.0.0.1:9001/moyun/2026/08/28/08c050c645394a3d98e19b681e18da59.jpg','http://127.0.0.1:9001/moyun/2026/08/28/08c050c645394a3d98e19b681e18da59.jpg','minio','moyun','2026/08/28/08c050c645394a3d98e19b681e18da59.jpg',0,NULL,'b39afe6c9cadbe4708327429b847b1f9',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'1'),
                                                                                                                                                                                                                                                                                               ('城市.png','png','image',60522,'http://127.0.0.1:9001/moyun/2026/08/28/9c9f3bb92f9a4c66810a04618a158597.png','http://127.0.0.1:9001/moyun/2026/08/28/9c9f3bb92f9a4c66810a04618a158597.png','minio','moyun','2026/08/28/9c9f3bb92f9a4c66810a04618a158597.png',0,NULL,'8f6f7563ab5a7fe5719cb875798b09d7',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('脱毛.png','png','image',20066,'http://127.0.0.1:9001/moyun/2026/08/28/3c8e81614b5b4e3391a82f5d24c49f65.png','http://127.0.0.1:9001/moyun/2026/08/28/3c8e81614b5b4e3391a82f5d24c49f65.png','minio','moyun','2026/08/28/3c8e81614b5b4e3391a82f5d24c49f65.png',0,NULL,'6ba26bf100d7c93743da9eab10a77d6d',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('轮播图1-正常.png','png','image',165435,'http://127.0.0.1:9001/moyun/2026/08/28/df35cc1281ec40dba5f3edbce706a3df.png','http://127.0.0.1:9001/moyun/2026/08/28/df35cc1281ec40dba5f3edbce706a3df.png','minio','moyun','2026/08/28/df35cc1281ec40dba5f3edbce706a3df.png',0,NULL,'557db10957d1faa37326be900eb6ba90',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('tutu.png','png','image',94236,'http://127.0.0.1:9001/moyun/2026/08/28/58f3433eea964d85a54954e19cea8187.png','http://127.0.0.1:9001/moyun/2026/08/28/58f3433eea964d85a54954e19cea8187.png','minio','moyun','2026/08/28/58f3433eea964d85a54954e19cea8187.png',0,NULL,'2216d21e52571c2e52b778d7fd55c23e',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('熊大.png','png','image',27324,'http://127.0.0.1:9001/moyun/2026/08/28/64959c2a7418433fbf6a940ee2b2d4c7.png','http://127.0.0.1:9001/moyun/2026/08/28/64959c2a7418433fbf6a940ee2b2d4c7.png','minio','moyun','2026/08/28/64959c2a7418433fbf6a940ee2b2d4c7.png',0,NULL,'b78ca092240c0631d496f75cf362d268',6,'zhangsan','0','article_cover',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('可爱.jpg','jpg','image',36835,'http://127.0.0.1:9001/moyun/2026/08/28/db4fb76bcdf94586b8cc73a03880254c.jpg','http://127.0.0.1:9001/moyun/2026/08/28/db4fb76bcdf94586b8cc73a03880254c.jpg','minio','moyun','2026/08/28/db4fb76bcdf94586b8cc73a03880254c.jpg',0,NULL,'b39afe6c9cadbe4708327429b847b1f9',6,'zhangsan','0','article_content',NULL,'',NULL,'',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_file (file_name,file_ext,file_type,file_size,file_url,file_path,storage_type,bucket_name,object_name,fallback,local_path,file_md5,upload_user_id,upload_user_name,status,business_type,business_id,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                                                                               ('tutu.png','png','image',94236,'http://127.0.0.1:9001/moyun/2026/08/28/5b6a82da1d7b443cb6e36cc66fc90b36.png','http://127.0.0.1:9001/moyun/2026/08/28/5b6a82da1d7b443cb6e36cc66fc90b36.png','minio','moyun','2026/08/28/5b6a82da1d7b443cb6e36cc66fc90b36.png',0,NULL,'2216d21e52571c2e52b778d7fd55c23e',6,'zhangsan','0','article_content',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('简历封面-01.png','png','image',335995,'http://127.0.0.1:9001/moyun/2026/08/31/d88a580a882f460e9f6f2e762097ae3f.png','http://127.0.0.1:9001/moyun/2026/08/31/d88a580a882f460e9f6f2e762097ae3f.png','minio','moyun','2026/08/31/d88a580a882f460e9f6f2e762097ae3f.png',0,NULL,'43c611a0b83b26b4f5d8248ad65763e2',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('简历封面-01.png','png','image',335995,'http://127.0.0.1:9001/moyun/2026/08/31/de169a8619444c9380cae4a20f5f9c50.png','http://127.0.0.1:9001/moyun/2026/08/31/de169a8619444c9380cae4a20f5f9c50.png','minio','moyun','2026/08/31/de169a8619444c9380cae4a20f5f9c50.png',0,NULL,'43c611a0b83b26b4f5d8248ad65763e2',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/08/31/df797bbb6ca044c1a06359cc08fe0e25.pdf','http://127.0.0.1:9001/moyun/2026/08/31/df797bbb6ca044c1a06359cc08fe0e25.pdf','minio','moyun','2026/08/31/df797bbb6ca044c1a06359cc08fe0e25.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/08/31/4c0903c5f4ad44858b0018c374b0aac0.pdf','http://127.0.0.1:9001/moyun/2026/08/31/4c0903c5f4ad44858b0018c374b0aac0.pdf','minio','moyun','2026/08/31/4c0903c5f4ad44858b0018c374b0aac0.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/08/31/f495d202049b4b558c843c662a4f0f0b.pdf','http://127.0.0.1:9001/moyun/2026/08/31/f495d202049b4b558c843c662a4f0f0b.pdf','minio','moyun','2026/08/31/f495d202049b4b558c843c662a4f0f0b.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/28c7643599bd45eebccf0763f74c69d9.pdf','http://127.0.0.1:9001/moyun/2026/09/01/28c7643599bd45eebccf0763f74c69d9.pdf','minio','moyun','2026/09/01/28c7643599bd45eebccf0763f74c69d9.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/aa1e3ae4b59f47d8baba55d49f49630c.pdf','http://127.0.0.1:9001/moyun/2026/09/01/aa1e3ae4b59f47d8baba55d49f49630c.pdf','minio','moyun','2026/09/01/aa1e3ae4b59f47d8baba55d49f49630c.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/420a1affcc1848b0bb95d9105f918cd0.pdf','http://127.0.0.1:9001/moyun/2026/09/01/420a1affcc1848b0bb95d9105f918cd0.pdf','minio','moyun','2026/09/01/420a1affcc1848b0bb95d9105f918cd0.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/f362589317454f638cc5e36100323981.pdf','http://127.0.0.1:9001/moyun/2026/09/01/f362589317454f638cc5e36100323981.pdf','minio','moyun','2026/09/01/f362589317454f638cc5e36100323981.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_file (file_name,file_ext,file_type,file_size,file_url,file_path,storage_type,bucket_name,object_name,fallback,local_path,file_md5,upload_user_id,upload_user_name,status,business_type,business_id,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/62b2e18f18354643bbbe1430dc284f80.pdf','http://127.0.0.1:9001/moyun/2026/09/01/62b2e18f18354643bbbe1430dc284f80.pdf','minio','moyun','2026/09/01/62b2e18f18354643bbbe1430dc284f80.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/02/f5830e65f81146ab8e4cbc09ea7c16f5.pdf','http://127.0.0.1:9001/moyun/2026/09/02/f5830e65f81146ab8e4cbc09ea7c16f5.pdf','minio','moyun','2026/09/02/f5830e65f81146ab8e4cbc09ea7c16f5.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('半身像1.jpg','jpg','image',614342,'http://127.0.0.1:9001/moyun/2026/09/02/a65c86424ed64626a710b4ecdc638a0c.jpg','http://127.0.0.1:9001/moyun/2026/09/02/a65c86424ed64626a710b4ecdc638a0c.jpg','minio','moyun','2026/09/02/a65c86424ed64626a710b4ecdc638a0c.jpg',0,NULL,'b7b2a77ef8c3afa447e7c8c4287bfc98',6,'zhangsan','0','avatar','6','',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('20260903栏目设置.jpeg','jpeg','image',334381,'http://127.0.0.1:9001/moyun/2026/09/07/37619d86ff7b474e9aacd4f737c84d9c.jpeg','http://127.0.0.1:9001/moyun/2026/09/07/37619d86ff7b474e9aacd4f737c84d9c.jpeg','minio','moyun','2026/09/07/37619d86ff7b474e9aacd4f737c84d9c.jpeg',0,NULL,'fe13d3b3d051b703c4c13e20d9087045',6,'zhangsan','0','article_cover',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf','http://127.0.0.1:9001/moyun/2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf','minio','moyun','2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0'),
                                                                                                                                                                                                                                                                                               ('弄谷村暮色.jpg','jpg','image',1688202,'http://127.0.0.1:9001/moyun/2026/09/07/59caaf2c79f94d9c804ddb6618cbfe7e.jpg','http://127.0.0.1:9001/moyun/2026/09/07/59caaf2c79f94d9c804ddb6618cbfe7e.jpg','minio','moyun','2026/09/07/59caaf2c79f94d9c804ddb6618cbfe7e.jpg',0,NULL,'b00b64d415166363ad23057f534d40e6',6,'zhangsan','0','topic_cover',NULL,'',NULL,'',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_job (job_name,job_group,invoke_target,cron_expression,misfire_policy,concurrent,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                   ('系统默认（无参）','DEFAULT','ryTask.ryNoParams','0/10 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),
                                                                                                                                                                                   ('系统默认（有参）','DEFAULT','ryTask.ryParams(''ry'')','0/15 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),
                                                                                                                                                                                   ('系统默认（多参）','DEFAULT','ryTask.ryMultipleParams(''ry'', true, 2000, 316.50, 100)','0/20 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),
                                                                                                                                                                                   ('敏感词扫描-话题','DEFAULT','sensitiveScanTask.scanTopics()','0 0 3 * * ?','3','1','0','admin','2026-08-19 18:01:41','',NULL,'定时扫描话题内容，命中则转待审核','0'),
                                                                                                                                                                                   ('敏感词扫描-观点','DEFAULT','sensitiveScanTask.scanTopicPosts()','0 5 3 * * ?','3','1','0','admin','2026-08-19 18:01:41','',NULL,'定时扫描话题观点，命中则标记','0'),
                                                                                                                                                                                   ('缓存清理-表结构','DEFAULT','cacheCleanupTask.cleanupExpiredCache()','0 */5 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每5分钟清理 DataSourceService 的过期表结构缓存（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）','0'),
                                                                                                                                                                                   ('缓存清理-限流器','DEFAULT','cacheCleanupTask.cleanupRateLimiter()','0 */2 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每2分钟清理 RateLimiter 过期数据（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）','0'),
                                                                                                                                                                                   ('会话清理-数据分析','DEFAULT','dataAnalysisConversationServiceImpl.cleanExpiredSessions()','0 */30 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每30分钟清理超过1小时未访问的数据分析对话会话（原 DataAnalysisConversationServiceImpl @Scheduled，迁移至 Quartz 统一调度）','0'),
                                                                                                                                                                                   ('日志落盘-Token使用','DEFAULT','tokenUsageServiceImpl.flushLogsToDB()','0 * * * * ?','1','1','0','admin','2026-08-19 18:01:42','',NULL,'每1分钟将 Redis 中的 Token 使用日志批量写入 DB（原 TokenUsageServiceImpl @Scheduled，迁移至 Quartz 统一调度；misfire=1 立即补偿避免日志丢失）','0'),
                                                                                                                                                                                   ('敏感词扫描-文章评论','DEFAULT','sensitiveScanTask.scanArticleComments()','0 10 3 * * ?','3','1','0','admin','2026-08-19 18:01:45','',NULL,'扫描已发布文章评论(portal_comment.status=1)，命中敏感词转驳回(status=2)并通知作者','0');
INSERT INTO `moyun-db`.sys_job (job_name,job_group,invoke_target,cron_expression,misfire_policy,concurrent,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                   ('敏感词扫描-面经评论','DEFAULT','sensitiveScanTask.scanInterviewComments()','0 15 3 * * ?','3','1','0','admin','2026-08-19 18:01:45','',NULL,'扫描已发布面经评论(portal_interview_comment.status=published)，命中敏感词转rejected并通知作者','0'),
                                                                                                                                                                                   ('AI每日写作Prompt生成','DEFAULT','writingPromptTask.generateDailyPrompt()','0 10 0 * * ?','3','1','0','admin','2026-08-25 00:00:00','',NULL,'每天00:10为今天+明天生成写作提示（结合节日节气，AI失败回退内置主题池）','0');
INSERT INTO `moyun-db`.sys_job_log (job_name,job_group,invoke_target,job_message,status,exception_info,create_time,create_by,update_by,update_time,remark) VALUES
    ('日志落盘-Token使用','DEFAULT','tokenUsageServiceImpl.flushLogsToDB()','日志落盘-Token使用 总共耗时：1毫秒','0','','2026-09-11 17:31:00','','',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-28 09:33:49','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-08-28 09:59:09','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-08-28 09:59:25','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('libai','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 10:02:39','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-08-28 10:56:52','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-28 10:57:45','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-08-28 11:42:08','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-28 13:09:07','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('libai','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 13:10:44','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 13:49:11','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 14:01:15','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 14:37:17','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 16:45:49','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-31 09:14:39','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 09:15:21','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 10:08:45','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-31 13:04:01','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 13:47:01','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 15:15:13','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-31 15:15:39','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 16:19:30','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 08:54:22','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-01 09:27:19','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 09:46:42','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 09:47:08','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 10:37:43','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-01 10:50:36','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-01 13:12:06','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 13:13:50','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 14:30:41','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-01 15:23:14','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 15:49:00','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 16:27:19','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-02 10:16:12','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-02 13:17:49','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-02 13:21:06','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-03 11:27:04','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 13:28:16','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('test01','127.0.0.1','内网IP','Chrome 14','Windows 10','1','portal','用户名或密码错误','2026-09-03 13:44:01','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('test01','127.0.0.1','内网IP','Chrome 14','Windows 10','1','portal','用户名或密码错误','2026-09-03 13:44:25','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('test01','127.0.0.1','内网IP','Chrome 14','Windows 10','1','portal','用户名或密码错误','2026-09-03 13:44:42','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('test01','127.0.0.1','内网IP','Chrome 14','Windows 10','1','portal','用户名或密码错误','2026-09-03 13:46:05','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 14:29:53','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-03 15:06:53','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-03 15:09:49','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 15:10:24','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 15:38:59','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-03 15:48:12','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-03 15:49:39','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 16:15:41','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 17:05:24','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 17:08:48','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-03 17:25:21','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-03 17:25:34','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-03 17:25:47','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-04 09:05:48','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-04 09:05:48','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-04 09:45:07','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-04 09:57:12','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:04:42','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:08','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:19','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:19','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:19','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:19','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:32:38','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:32:52','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:33:02','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:23','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:32','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:39','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:48','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:54','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:41:28','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:42:06','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:45:11','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:45:21','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:45:30','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:49:21','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:49:56','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('libai','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 12:50:55','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 14:34:30','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 14:35:50','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 14:38:25','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-04 14:39:14','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-04 16:29:54','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 16:47:48','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 17:19:38','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 17:35:58','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-07 09:17:49','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-07 09:52:18','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 09:53:49','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-07 11:18:32','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:18:59','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:27:30','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:29:05','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:30:41','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:40:00','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-07 14:33:49','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 15:35:00','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 16:15:23','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-07 17:17:17','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 17:38:22','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 09:06:13','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 10:10:32','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-08 11:40:48','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-08 11:42:12','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 13:08:08','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 14:16:06','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-08 14:24:08','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-08 14:24:25','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 17:08:25','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-09 11:32:15','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-09 11:37:39','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-09 13:56:00','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('lisi','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-09 15:31:20','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-09 17:54:05','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-09 17:56:03','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-10 09:02:17','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-10 10:19:44','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-10 10:48:57','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-10 13:06:49','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-10 13:11:58','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-10 15:25:59','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-10 17:00:57','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-10 18:12:18','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-11 08:56:43','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-11 09:22:48','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-11 13:32:21','',NULL,'',NULL,NULL),
                                                                                                                                                                          ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-11 16:23:28','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('系统设置',0,7,'system',NULL,'','',1,0,'M','0','0','','system','admin','2026-08-19 18:01:45','',NULL,'系统管理目录','0'),
                                                                                                                                                                                                                        ('系统监控',1,5,'monitor',NULL,'','',1,0,'M','0','0','','monitor','admin','2026-08-19 18:01:45','',NULL,'系统监控目录','0'),
                                                                                                                                                                                                                        ('系统工具',1,6,'tool',NULL,'','',1,0,'M','0','0','','tool','admin','2026-08-19 18:01:45','',NULL,'系统工具目录','0'),
                                                                                                                                                                                                                        ('用户管理',5243,1,'user','system/user/index','','',1,0,'C','0','0','system:user:list','user','admin','2026-08-19 18:01:45','',NULL,'用户管理菜单','0'),
                                                                                                                                                                                                                        ('角色管理',5243,2,'role','system/role/index','','',1,0,'C','0','0','system:role:list','peoples','admin','2026-08-19 18:01:45','',NULL,'角色管理菜单','0'),
                                                                                                                                                                                                                        ('菜单管理',5243,3,'menu','system/menu/index','','',1,0,'C','0','0','system:menu:list','tree-table','admin','2026-08-19 18:01:45','',NULL,'菜单管理菜单','0'),
                                                                                                                                                                                                                        ('部门管理',5243,4,'dept','system/dept/index','','',1,0,'C','0','0','system:dept:list','tree','admin','2026-08-19 18:01:45','',NULL,'部门管理菜单','0'),
                                                                                                                                                                                                                        ('岗位管理',5243,5,'post','system/post/index','','',1,0,'C','0','0','system:post:list','post','admin','2026-08-19 18:01:45','',NULL,'岗位管理菜单','0'),
                                                                                                                                                                                                                        ('字典管理',5244,1,'dict','system/dict/index','','',1,0,'C','0','0','system:dict:list','dict','admin','2026-08-19 18:01:45','',NULL,'字典管理菜单','0'),
                                                                                                                                                                                                                        ('参数设置',5244,2,'config','system/config/index','','',1,0,'C','0','0','system:config:list','edit','admin','2026-08-19 18:01:45','',NULL,'参数设置菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('日志管理',1,3,'log','','','',1,0,'M','0','0','','log','admin','2026-08-19 18:01:45','',NULL,'日志管理菜单','0'),
                                                                                                                                                                                                                        ('在线用户',2,1,'online','monitor/online/index','','',1,0,'C','0','0','monitor:online:list','online','admin','2026-08-19 18:01:45','',NULL,'在线用户菜单','0'),
                                                                                                                                                                                                                        ('定时任务',2,2,'job','monitor/job/index','','',1,0,'C','0','0','monitor:job:list','job','admin','2026-08-19 18:01:45','',NULL,'定时任务菜单','0'),
                                                                                                                                                                                                                        ('数据监控',2,3,'druid','monitor/druid/index','','',1,0,'C','0','0','monitor:druid:list','druid','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','数据监控菜单','0'),
                                                                                                                                                                                                                        ('服务监控',2,4,'server','monitor/server/index','','',1,0,'C','0','0','monitor:server:list','server','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','服务监控菜单','0'),
                                                                                                                                                                                                                        ('缓存监控',2,5,'cache','monitor/cache/index','','',1,0,'C','0','0','monitor:cache:list','redis','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','缓存监控菜单','0'),
                                                                                                                                                                                                                        ('缓存列表',2,6,'cacheList','monitor/cache/list','','',1,0,'C','0','0','monitor:cache:list','redis-list','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','缓存列表菜单','0'),
                                                                                                                                                                                                                        ('代码生成',3,2,'gen','tool/gen/index','','',1,0,'C','0','0','tool:gen:list','code','admin','2026-08-19 18:01:45','',NULL,'代码生成菜单','0'),
                                                                                                                                                                                                                        ('接口文档',3,3,'swagger','tool/swagger/index','','',1,0,'C','0','0','tool:swagger:list','swagger','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','系统接口菜单','0'),
                                                                                                                                                                                                                        ('操作日志',108,1,'operlog','monitor/operlog/index','','',1,0,'C','0','0','monitor:operlog:list','form','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','操作日志菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('登录日志',108,2,'logininfor','monitor/logininfor/index','','',1,0,'C','0','0','monitor:logininfor:list','logininfor','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','登录日志菜单','0'),
                                                                                                                                                                                                                        ('用户查询',100,1,'','','','',1,0,'F','0','0','system:user:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('用户新增',100,2,'','','','',1,0,'F','0','0','system:user:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('用户修改',100,3,'','','','',1,0,'F','0','0','system:user:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('用户删除',100,4,'','','','',1,0,'F','0','0','system:user:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('用户导出',100,5,'','','','',1,0,'F','0','0','system:user:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('用户导入',100,6,'','','','',1,0,'F','0','0','system:user:import','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('重置密码',100,7,'','','','',1,0,'F','0','0','system:user:resetPwd','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('角色查询',101,1,'','','','',1,0,'F','0','0','system:role:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('角色新增',101,2,'','','','',1,0,'F','0','0','system:role:add','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('角色修改',101,3,'','','','',1,0,'F','0','0','system:role:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('角色删除',101,4,'','','','',1,0,'F','0','0','system:role:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('角色导出',101,5,'','','','',1,0,'F','0','0','system:role:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('菜单查询',102,1,'','','','',1,0,'F','0','0','system:menu:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('菜单新增',102,2,'','','','',1,0,'F','0','0','system:menu:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('菜单修改',102,3,'','','','',1,0,'F','0','0','system:menu:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('菜单删除',102,4,'','','','',1,0,'F','0','0','system:menu:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('部门查询',103,1,'','','','',1,0,'F','0','0','system:dept:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('部门新增',103,2,'','','','',1,0,'F','0','0','system:dept:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('部门修改',103,3,'','','','',1,0,'F','0','0','system:dept:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('部门删除',103,4,'','','','',1,0,'F','0','0','system:dept:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('岗位查询',104,1,'','','','',1,0,'F','0','0','system:post:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('岗位新增',104,2,'','','','',1,0,'F','0','0','system:post:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('岗位修改',104,3,'','','','',1,0,'F','0','0','system:post:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('岗位删除',104,4,'','','','',1,0,'F','0','0','system:post:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('岗位导出',104,5,'','','','',1,0,'F','0','0','system:post:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('字典查询',105,1,'#','','','',1,0,'F','0','0','system:dict:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('字典新增',105,2,'#','','','',1,0,'F','0','0','system:dict:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('字典修改',105,3,'#','','','',1,0,'F','0','0','system:dict:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('字典删除',105,4,'#','','','',1,0,'F','0','0','system:dict:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('字典导出',105,5,'#','','','',1,0,'F','0','0','system:dict:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('参数查询',106,1,'#','','','',1,0,'F','0','0','system:config:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('参数新增',106,2,'#','','','',1,0,'F','0','0','system:config:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('参数修改',106,3,'#','','','',1,0,'F','0','0','system:config:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('参数删除',106,4,'#','','','',1,0,'F','0','0','system:config:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('参数导出',106,5,'#','','','',1,0,'F','0','0','system:config:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('操作查询',500,1,'#','','','',1,0,'F','0','0','monitor:operlog:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('操作删除',500,2,'#','','','',1,0,'F','0','0','monitor:operlog:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('日志导出',500,3,'#','','','',1,0,'F','0','0','monitor:operlog:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('登录查询',501,1,'#','','','',1,0,'F','0','0','monitor:logininfor:query','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('登录删除',501,2,'#','','','',1,0,'F','0','0','monitor:logininfor:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('日志导出',501,3,'#','','','',1,0,'F','0','0','monitor:logininfor:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('账号解锁',501,4,'#','','','',1,0,'F','0','0','monitor:logininfor:unlock','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('在线查询',109,1,'#','','','',1,0,'F','0','0','monitor:online:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('批量强退',109,2,'#','','','',1,0,'F','0','0','monitor:online:batchLogout','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('单条强退',109,3,'#','','','',1,0,'F','0','0','monitor:online:forceLogout','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('任务查询',110,1,'#','','','',1,0,'F','0','0','monitor:job:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('任务新增',110,2,'#','','','',1,0,'F','0','0','monitor:job:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('任务修改',110,3,'#','','','',1,0,'F','0','0','monitor:job:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('任务删除',110,4,'#','','','',1,0,'F','0','0','monitor:job:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('状态修改',110,5,'#','','','',1,0,'F','0','0','monitor:job:changeStatus','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('任务导出',110,6,'#','','','',1,0,'F','0','0','monitor:job:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('生成查询',116,1,'#','','','',1,0,'F','0','0','tool:gen:query','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
                                                                                                                                                                                                                        ('生成修改',116,2,'#','','','',1,0,'F','0','0','tool:gen:edit','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
                                                                                                                                                                                                                        ('生成删除',116,3,'#','','','',1,0,'F','0','0','tool:gen:remove','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
                                                                                                                                                                                                                        ('导入代码',116,4,'#','','','',1,0,'F','0','0','tool:gen:import','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
                                                                                                                                                                                                                        ('预览代码',116,5,'#','','','',1,0,'F','0','0','tool:gen:preview','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
                                                                                                                                                                                                                        ('生成代码',116,6,'#','','','',1,0,'F','0','0','tool:gen:code','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
                                                                                                                                                                                                                        ('AI 能力',0,6,'ai',NULL,NULL,'',1,0,'M','0','0',NULL,'chart','admin','2026-08-19 18:01:40','',NULL,'智能AI目录','0'),
                                                                                                                                                                                                                        ('智能体管理',5000,1,'agent','ai/agent/index',NULL,'',1,0,'C','0','0','cms:ai:agent:list','edit','admin','2026-08-19 18:01:40','',NULL,'智能体管理菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('智能体查询',5001,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('智能体新增',5001,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('智能体修改',5001,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('智能体删除',5001,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('智能体测试',5001,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:test','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('知识库管理',5237,1,'knowledge-base','ai/knowledge-base/index',NULL,'',1,0,'C','0','0','cms:ai:knowledge-base:list','documentation','admin','2026-08-19 18:01:40','',NULL,'知识库管理菜单 [v7.16 已整合到知识中心 Tab 容器]','0'),
                                                                                                                                                                                                                        ('知识库查询',5007,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('知识库新增',5007,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('知识库修改',5007,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('知识库删除',5007,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('文档上传',5007,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:upload','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('知识文库',5237,2,'knowledge-library','ai/knowledge-library/index',NULL,'',1,0,'C','0','0','cms:ai:knowledge-library:list','tree-table','admin','2026-08-19 18:01:40','',NULL,'知识文库菜单 [v7.16 已整合到知识中心 Tab 容器]','0'),
                                                                                                                                                                                                                        ('文库查询',5013,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-library:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('文库新增',5013,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-library:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('文库修改',5013,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-library:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('文库删除',5013,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-library:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('模型配置',5238,1,'model-config','ai/model-config/index',NULL,'',1,0,'C','0','0','cms:ai:model-config:list','monitor','admin','2026-08-19 18:01:40','',NULL,'模型配置菜单','0'),
                                                                                                                                                                                                                        ('模型查询',5018,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('模型新增',5018,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('模型修改',5018,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('模型删除',5018,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('连接测试',5018,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:test','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('工具管理',5238,2,'tool','ai/tool/index',NULL,'',1,0,'C','0','0','cms:ai:tool:list','tool','admin','2026-08-19 18:01:40','',NULL,'工具管理菜单','0'),
                                                                                                                                                                                                                        ('工具查询',5024,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:tool:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('工具新增',5024,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:tool:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('工具修改',5024,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:tool:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('工具删除',5024,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:tool:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('工作流管理',5000,3,'workflow','ai/workflow/index',NULL,'',1,0,'C','0','0','cms:ai:workflow:list','chart','admin','2026-08-19 18:01:40','',NULL,'工作流管理菜单','0'),
                                                                                                                                                                                                                        ('工作流查询',5029,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('工作流新增',5029,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:add','#','admin','2026-08-19 18:01:40','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('工作流修改',5029,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('工作流删除',5029,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('工作流执行',5029,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:execute','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('AI生成工作流',5029,6,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow-generator:generate','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('领域词典',5000,5,'dictionary','ai/dictionary/index',NULL,'',1,0,'C','0','0','cms:ai:domain-dictionary:list','dict','admin','2026-08-19 18:01:40','',NULL,'领域词典菜单','0'),
                                                                                                                                                                                                                        ('词典查询',5036,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:domain-dictionary:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('词典新增',5036,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:domain-dictionary:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('词典修改',5036,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:domain-dictionary:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('词典删除',5036,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:domain-dictionary:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('数据源管理',5238,3,'datasource','ai/datasource/index',NULL,'',1,0,'C','0','0','cms:ai:datasource:list','druid','admin','2026-08-19 18:01:40','',NULL,'数据源管理菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('数据源查询',5041,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('数据源新增',5041,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('数据源修改',5041,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('数据源删除',5041,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('连接测试',5041,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:test','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('元数据同步',5041,6,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:sync','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('Token统计',5239,2,'token-usage','ai/token-usage/index',NULL,'',1,0,'C','0','0','cms:ai:token-usage:list','money','admin','2026-08-19 18:01:40','',NULL,'Token统计菜单','0'),
                                                                                                                                                                                                                        ('统计查询',5048,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:token-usage:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('统计导出',5048,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:token-usage:export','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('AI数据分析',5000,6,'query','ai/query/index',NULL,'',1,0,'C','0','0','cms:ai:data-analysis:list','icon','admin','2026-08-19 18:01:40','',NULL,'智能数据分析菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('查询查询',5051,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:data-analysis:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('SQL生成',5051,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:data-analysis:sql','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('报告生成',5051,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:data-analysis:report','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('图表生成',5051,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:diagram:generate','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
                                                                                                                                                                                                                        ('概览大屏',5239,1,'ai-dashboard','ai/dashboard/index',NULL,'',1,0,'C','0','0','cms:ai:dashboard:list','chart','admin','2026-08-19 18:01:40','',NULL,'AI数据大屏菜单','0'),
                                                                                                                                                                                                                        ('架构图生成',5000,7,'diagram/chat','ai/diagram/chat',NULL,'',1,0,'C','0','0','cms:ai:diagram:list','build','admin','2026-08-19 18:01:40','',NULL,'AI架构图生成菜单','0'),
                                                                                                                                                                                                                        ('智能对话使用',5001,10,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:chat:list','#','admin','2026-08-19 18:01:40','',NULL,'AI对话/中断/重新生成接口权限','0'),
                                                                                                                                                                                                                        ('话题管理',5068,14,'topic','cms/topic/index',NULL,'',1,0,'C','0','0','cms:topic:list','message','admin','2026-08-19 18:01:41','admin','2026-08-20 13:39:28','话题列表与状态管理','0'),
                                                                                                                                                                                                                        ('话题查询',5059,1,'#','',NULL,'',1,0,'F','0','0','cms:topic:query','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('敏感词管理',5244,3,'sensitiveWord','system/sensitiveWord/index',NULL,'',1,0,'C','0','0','system:sensitiveWord:list','dict','admin','2026-08-19 18:01:41','admin','2026-08-20 13:34:56','敏感词库维护与词树刷新','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('敏感词查询',5063,1,'#','',NULL,'',1,0,'F','0','0','system:sensitiveWord:query','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('敏感词新增',5063,2,'#','',NULL,'',1,0,'F','0','0','system:sensitiveWord:add','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('敏感词修改',5063,3,'#','',NULL,'',1,0,'F','0','0','system:sensitiveWord:edit','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('敏感词删除',5063,4,'#','',NULL,'',1,0,'F','0','0','system:sensitiveWord:remove','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('内容管理',5241,1,'cms',NULL,NULL,'',1,0,'M','0','0',NULL,'documentation','admin','2026-08-19 18:01:45','',NULL,'内容管理目录','0'),
                                                                                                                                                                                                                        ('门户用户',5068,1,'portal-user','cms/user/index',NULL,'',1,0,'C','0','0','cms:user:list','user','admin','2026-08-19 18:01:45','',NULL,'门户用户管理菜单','0'),
                                                                                                                                                                                                                        ('用户查询',5069,1,'',NULL,NULL,'',1,0,'F','0','0','cms:user:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('用户新增',5069,2,'',NULL,NULL,'',1,0,'F','0','0','cms:user:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('用户修改',5069,3,'',NULL,NULL,'',1,0,'F','0','0','cms:user:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('用户删除',5069,4,'',NULL,NULL,'',1,0,'F','0','0','cms:user:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('用户状态',5069,5,'',NULL,NULL,'',1,0,'F','0','0','cms:user:status','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('重置密码',5069,6,'',NULL,NULL,'',1,0,'F','0','0','cms:user:resetPwd','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('绑定系统用户',5069,7,'',NULL,NULL,'',1,0,'F','0','0','cms:user:bind','#','admin','2026-08-19 18:01:45','',NULL,'身份桥接：绑定/解绑后台系统用户','0'),
                                                                                                                                                                                                                        ('文章管理',5068,2,'article','cms/article/index',NULL,'',1,0,'C','0','0','cms:article:list','edit','admin','2026-08-19 18:01:45','',NULL,'文章管理菜单','0'),
                                                                                                                                                                                                                        ('文章查询',5077,1,'',NULL,NULL,'',1,0,'F','0','0','cms:article:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('文章新增',5077,2,'',NULL,NULL,'',1,0,'F','0','0','cms:article:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('文章修改',5077,3,'',NULL,NULL,'',1,0,'F','0','0','cms:article:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('文章删除',5077,4,'',NULL,NULL,'',1,0,'F','0','0','cms:article:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('文章审核',5077,5,'',NULL,NULL,'',1,0,'F','0','0','cms:article:audit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('文章上架',5077,6,'',NULL,NULL,'',1,0,'F','0','0','cms:article:publish','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('文章推荐',5077,7,'',NULL,NULL,'',1,0,'F','0','0','cms:article:featured','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类管理',5068,3,'/category','cms/category/index',NULL,'',1,0,'C','0','0','cms:category:list','tree','admin','2026-08-19 18:01:45','',NULL,'分类管理菜单','0'),
                                                                                                                                                                                                                        ('分类查询',5085,1,'',NULL,NULL,'',1,0,'F','0','0','cms:category:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类新增',5085,2,'',NULL,NULL,'',1,0,'F','0','0','cms:category:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类修改',5085,3,'',NULL,NULL,'',1,0,'F','0','0','cms:category:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类删除',5085,4,'',NULL,NULL,'',1,0,'F','0','0','cms:category:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('标签管理',5068,4,'tag','cms/tag/index',NULL,'',1,0,'C','0','0','cms:tag:list','tab','admin','2026-08-19 18:01:45','',NULL,'标签管理菜单','0'),
                                                                                                                                                                                                                        ('标签查询',5090,1,'',NULL,NULL,'',1,0,'F','0','0','cms:tag:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('标签新增',5090,2,'',NULL,NULL,'',1,0,'F','0','0','cms:tag:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('标签修改',5090,3,'',NULL,NULL,'',1,0,'F','0','0','cms:tag:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('标签删除',5090,4,'',NULL,NULL,'',1,0,'F','0','0','cms:tag:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('评论管理',5068,5,'comment','cms/comment/index',NULL,'',1,0,'C','0','0','cms:comment:list','message','admin','2026-08-19 18:01:45','',NULL,'评论管理菜单','0'),
                                                                                                                                                                                                                        ('评论查询',5095,1,'',NULL,NULL,'',1,0,'F','0','0','cms:comment:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('评论审核',5095,2,'',NULL,NULL,'',1,0,'F','0','0','cms:comment:audit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('评论删除',5095,3,'',NULL,NULL,'',1,0,'F','0','0','cms:comment:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('友情链接',5068,7,'friend-link','cms/friend-link/index',NULL,'',1,0,'C','0','0','cms:friend-link:list','link','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','友情链接管理菜单','0'),
                                                                                                                                                                                                                        ('友情链接查询',5099,1,'',NULL,NULL,'',1,0,'F','0','0','cms:friend-link:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('友情链接新增',5099,2,'',NULL,NULL,'',1,0,'F','0','0','cms:friend-link:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('友情链接修改',5099,3,'',NULL,NULL,'',1,0,'F','0','0','cms:friend-link:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('友情链接删除',5099,4,'',NULL,NULL,'',1,0,'F','0','0','cms:friend-link:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('帮助分类',5068,8,'help-category','cms/help-category/index',NULL,'',1,0,'C','1','0','cms:help-category:list','tree','admin','2026-08-19 18:01:45','admin','2026-08-25 00:00:00','已合并至帮助中心(5146)，菜单隐藏保留路由','0'),
                                                                                                                                                                                                                        ('分类查询',5104,1,'',NULL,NULL,'',1,0,'F','0','0','cms:help-category:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类新增',5104,2,'',NULL,NULL,'',1,0,'F','0','0','cms:help-category:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类修改',5104,3,'',NULL,NULL,'',1,0,'F','0','0','cms:help-category:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类删除',5104,4,'',NULL,NULL,'',1,0,'F','0','0','cms:help-category:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('帮助文章',5068,9,'help-article','cms/help-article/index',NULL,'',1,0,'C','1','0','cms:help-article:list','documentation','admin','2026-08-19 18:01:45','admin','2026-08-25 00:00:00','已合并至帮助中心(5146)，菜单隐藏保留路由','0'),
                                                                                                                                                                                                                        ('文章查询',5109,1,'',NULL,NULL,'',1,0,'F','0','0','cms:help-article:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('文章新增',5109,2,'',NULL,NULL,'',1,0,'F','0','0','cms:help-article:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('文章修改',5109,3,'',NULL,NULL,'',1,0,'F','0','0','cms:help-article:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('文章删除',5109,4,'',NULL,NULL,'',1,0,'F','0','0','cms:help-article:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('举报管理',5068,10,'report','cms/report/index',NULL,'',1,0,'C','0','0','cms:report:list','warning','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','用户举报记录管理','0'),
                                                                                                                                                                                                                        ('举报查询',5114,1,'',NULL,NULL,'',1,0,'F','0','0','cms:report:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('处理举报',5114,2,'',NULL,NULL,'',1,0,'F','0','0','cms:report:handle','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('删除举报',5114,3,'',NULL,NULL,'',1,0,'F','0','0','cms:report:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('反馈管理',5068,11,'feedback','cms/feedback/index',NULL,'',1,0,'C','0','0','cms:feedback:list','message','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','用户意见反馈管理','0'),
                                                                                                                                                                                                                        ('反馈查询',5118,1,'',NULL,NULL,'',1,0,'F','0','0','cms:feedback:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('处理反馈',5118,2,'',NULL,NULL,'',1,0,'F','0','0','cms:feedback:handle','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('删除反馈',5118,3,'',NULL,NULL,'',1,0,'F','0','0','cms:feedback:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('专栏管理',5068,12,'column','cms/column/index',NULL,'',1,0,'C','1','0','portal:column:list','documentation','admin','2026-08-19 18:01:45','admin','2026-08-31 15:03:16','专栏后台管理菜单 [v8.1 隐藏：合并到文章管理 Tab，路由保留供 Tab 组件复用]','0'),
                                                                                                                                                                                                                        ('专栏查询',5122,1,'',NULL,NULL,'',1,0,'F','0','0','portal:column:query','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('专栏新增',5122,2,'',NULL,NULL,'',1,0,'F','0','0','portal:column:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('专栏修改',5122,3,'',NULL,NULL,'',1,0,'F','0','0','portal:column:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('专栏删除',5122,4,'',NULL,NULL,'',1,0,'F','0','0','portal:column:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('创作者认证',5241,2,'certification',NULL,NULL,'',1,0,'M','1','0',NULL,'user','admin','2026-08-19 18:01:45','',NULL,'创作者认证目录','0'),
                                                                                                                                                                                                                        ('认证审核',5127,1,'audit','cms/certification/index',NULL,'',1,0,'C','1','0','cms:certification:audit','edit','admin','2026-08-19 18:01:45','',NULL,'创作者认证审核菜单','0'),
                                                                                                                                                                                                                        ('认证查询',5128,1,'',NULL,NULL,'',1,0,'F','1','0','cms:certification:list','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('认证审核',5128,2,'',NULL,NULL,'',1,0,'F','1','0','cms:certification:audit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('消息中心',5245,1,'message','system/message/index',NULL,'',1,0,'C','0','0','system:message:list','message','admin','2026-08-19 18:01:45','',NULL,'消息中心菜单（私信+通知双Tab）','0'),
                                                                                                                                                                                                                        ('私信查询',5134,1,'',NULL,NULL,'',1,0,'F','0','0','system:message:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('私信发送',5134,2,'',NULL,NULL,'',1,0,'F','0','0','system:message:send','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('通知查询',5134,3,'',NULL,NULL,'',1,0,'F','0','0','system:notification:list','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('通知管理',5245,2,'notification','system/notification/index',NULL,'',1,0,'C','0','0','system:notification:list','email','admin','2026-08-19 18:01:45','',NULL,'通知管理菜单（台账）','0'),
                                                                                                                                                                                                                        ('通知查询',5138,1,'',NULL,NULL,'',1,0,'F','0','0','system:notification:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('通知新增',5138,2,'',NULL,NULL,'',1,0,'F','0','0','system:notification:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('通知修改',5138,3,'',NULL,NULL,'',1,0,'F','0','0','system:notification:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('通知删除',5138,4,'',NULL,NULL,'',1,0,'F','0','0','system:notification:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('发送广播通知',5138,5,'',NULL,NULL,'',1,0,'F','0','0','system:notification:sendAll','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
                                                                                                                                                                                                                        ('推广位管理',5068,20,'promotion','cms/promotion/index',NULL,'',1,0,'C','0','0','cms:promotion:list','component','admin','2026-08-19 18:01:45','',NULL,'广告位与友情链接合并管理（Tab）','0'),
                                                                                                                                                                                                                        ('用户反馈处理',5068,21,'feedback-center','cms/feedback-center/index',NULL,'',1,0,'C','0','0','cms:feedback-center:list','message','admin','2026-08-19 18:01:45','',NULL,'反馈与举报合并处理（Tab）','0'),
                                                                                                                                                                                                                        ('帮助中心',5068,22,'help-center','cms/help-center/index',NULL,'',1,0,'C','0','0','cms:help-center:list','question','admin','2026-08-19 18:01:45','',NULL,'帮助分类与文章合并管理（Tab）','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('成长配置',5068,23,'growth-config','cms/growth-config/index',NULL,'',1,0,'C','0','0','cms:growth-config:list','star','admin','2026-08-19 18:01:45','',NULL,'成长规则与成就合并配置（Tab）','0'),
                                                                                                                                                                                                                        ('缓存管理',2,7,'cache-manage','monitor/cache-manage/index',NULL,'',1,0,'C','0','0','monitor:cache-manage:list','redis','admin','2026-08-19 18:01:45','',NULL,'缓存监控与列表合并管理（Tab）','0'),
                                                                                                                                                                                                                        ('日志审计',108,3,'log-audit','monitor/log-audit/index',NULL,'',1,0,'C','0','0','monitor:log-audit:list','log','admin','2026-08-19 18:01:45','',NULL,'操作日志与登录日志合并查询（Tab）','0'),
                                                                                                                                                                                                                        ('内容审核中心',5068,25,'audit-center','cms/audit-center/index',NULL,'',1,0,'C','1','0','cms:audit-center:list','check','admin','2026-08-19 18:01:45','',NULL,'文章/专栏/话题审核合并入口（Tab，嵌入模式）','0'),
                                                                                                                                                                                                                        ('服务监控',2,3,'server-panel','monitor/server-panel/index',NULL,'',1,0,'C','0','0','monitor:server-panel:list','monitor','admin','2026-08-19 18:01:45','',NULL,'服务器监控与数据监控(Druid)合并查看（Tab）','0'),
                                                                                                                                                                                                                        ('面试管理',5241,4,'interview','',NULL,'',1,0,'M','0','0',NULL,'guide','admin','2026-08-19 18:01:46','',NULL,'面试指南一级目录：题库/面经/简历/笔记 | V10.5: 降级为门户管理下二级目录，重命名为 面试管理','0'),
                                                                                                                                                                                                                        ('题库资源',5192,1,'questionTab','cms/interview/questionTab/index',NULL,'',1,0,'C','0','0','cms:interview:list','tree-table','admin','2026-08-19 18:01:46','',NULL,'面试题库+分类+公司标签+简历模板 Tab 容器','0'),
                                                                                                                                                                                                                        ('面经运营',5192,2,'experienceTab','cms/interview/experienceTab/index',NULL,'',1,0,'C','0','0','cms:interview:experience:list','edit','admin','2026-08-19 18:01:46','',NULL,'面经管理+评论管理 Tab 容器；审核入口在内容审核中心','0'),
                                                                                                                                                                                                                        ('精选笔记',5192,3,'submission','cms/interview/submission/index',NULL,'',1,0,'C','0','0','cms:interview:submission:list','star','admin','2026-08-19 18:01:46','',NULL,'精选笔记采纳与取消','0'),
                                                                                                                                                                                                                        ('题库查询',5193,1,'#','',NULL,'',1,0,'F','0','0','cms:interview:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('题库新增',5193,2,'#','',NULL,'',1,0,'F','0','0','cms:interview:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('题库修改',5193,3,'#','',NULL,'',1,0,'F','0','0','cms:interview:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：审核/置顶/精选采纳等运营操作','0'),
                                                                                                                                                                                                                        ('题库删除',5193,4,'#','',NULL,'',1,0,'F','0','0','cms:interview:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('面经查询',5194,1,'#','',NULL,'',1,0,'F','0','0','cms:interview:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('面经修改',5194,2,'#','',NULL,'',1,0,'F','0','0','cms:interview:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：审核/置顶/评论管理','0'),
                                                                                                                                                                                                                        ('面经删除',5194,3,'#','',NULL,'',1,0,'F','0','0','cms:interview:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('笔记查询',5195,1,'#','',NULL,'',1,0,'F','0','0','cms:interview:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('笔记修改',5195,2,'#','',NULL,'',1,0,'F','0','0','cms:interview:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：采纳/取消精选','0'),
                                                                                                                                                                                                                        ('学习管理',5241,5,'book','',NULL,'',1,0,'M','0','0',NULL,'education','admin','2026-08-19 18:01:46','',NULL,'读书空间一级目录：书籍/书单/金句/学习 | V10.5: 降级为门户管理下二级目录，重命名为 学习管理','0'),
                                                                                                                                                                                                                        ('书籍管理',5205,1,'book-index','portal/book/index',NULL,'',1,0,'C','0','0','portal:book:list','documentation','admin','2026-08-19 18:01:46','',NULL,'书籍CRUD + 章节导入向导（章节管理为隐藏子路由）','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('章节管理',5205,2,'bookChapter','portal/bookChapter/index',NULL,'',1,0,'C','0','0','portal:bookChapter:list','#','admin','2026-08-19 18:01:46','',NULL,'书籍章节CRUD + 发布/批量导入（隐藏菜单，从书籍详情跳转）','0'),
                                                                                                                                                                                                                        ('书单&推荐位',5205,3,'bookListTab','portal/bookListTab/index',NULL,'',1,0,'C','0','0','portal:bookList:list','list','admin','2026-08-19 18:01:46','',NULL,'书单管理+推荐位管理 Tab 容器','0'),
                                                                                                                                                                                                                        ('用户内容',5205,4,'userContent','portal/userContent/index',NULL,'',1,0,'C','0','0','portal:bookQuote:list','peoples','admin','2026-08-19 18:01:46','',NULL,'金句摘录+书架管理 Tab 容器','0'),
                                                                                                                                                                                                                        ('学习辅助',5205,9,'learn-aux','portal/learn-aux/index',NULL,'',1,0,'C','0','0','portal:learn:list','skill','admin','2026-08-19 18:01:46','admin','2026-08-19 18:01:46','v9.5: 迁移至面试指南（错题本/学习计划数据源于题库刷题）','0'),
                                                                                                                                                                                                                        ('书籍查询',5206,1,'#','',NULL,'',1,0,'F','0','0','portal:book:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('书籍新增',5206,2,'#','',NULL,'',1,0,'F','0','0','portal:book:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('书籍修改',5206,3,'#','',NULL,'',1,0,'F','0','0','portal:book:edit','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('书籍删除',5206,4,'#','',NULL,'',1,0,'F','0','0','portal:book:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('章节查询',5207,1,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('章节新增',5207,2,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('章节修改',5207,3,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:edit','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('章节删除',5207,4,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('章节发布',5207,5,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:publish','#','admin','2026-08-19 18:01:46','',NULL,'章节发布/撤回','0'),
                                                                                                                                                                                                                        ('书单查询',5208,1,'#','',NULL,'',1,0,'F','0','0','portal:bookList:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('书单新增',5208,2,'#','',NULL,'',1,0,'F','0','0','portal:bookList:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('书单修改',5208,3,'#','',NULL,'',1,0,'F','0','0','portal:bookList:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：管理书籍（增删排序）','0'),
                                                                                                                                                                                                                        ('书单删除',5208,4,'#','',NULL,'',1,0,'F','0','0','portal:bookList:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('推荐位列表',5208,5,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:list','#','admin','2026-08-19 18:01:46','',NULL,'Tab 内推荐位面板列表权限','0'),
                                                                                                                                                                                                                        ('推荐位查询',5208,6,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('推荐位新增',5208,7,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('推荐位修改',5208,8,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：上下架/排序','0'),
                                                                                                                                                                                                                        ('推荐位删除',5208,9,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('金句查询',5209,1,'#','',NULL,'',1,0,'F','0','0','portal:bookQuote:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('金句新增',5209,2,'#','',NULL,'',1,0,'F','0','0','portal:bookQuote:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('金句修改',5209,3,'#','',NULL,'',1,0,'F','0','0','portal:bookQuote:edit','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('金句删除',5209,4,'#','',NULL,'',1,0,'F','0','0','portal:bookQuote:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
                                                                                                                                                                                                                        ('书架列表',5209,5,'#','',NULL,'',1,0,'F','0','0','portal:bookshelf:list','#','admin','2026-08-19 18:01:46','',NULL,'Tab 内书架面板列表权限','0'),
                                                                                                                                                                                                                        ('书架移除',5209,6,'#','',NULL,'',1,0,'F','0','0','portal:bookshelf:remove','#','admin','2026-08-19 18:01:46','',NULL,'移出书架','0'),
                                                                                                                                                                                                                        ('学习计划查询',5210,1,'#','',NULL,'',1,0,'F','0','0','portal:studyPlan:list','#','admin','2026-08-19 18:01:46','',NULL,'学习计划只读列表','0'),
                                                                                                                                                                                                                        ('错题本查询',5210,2,'#','',NULL,'',1,0,'F','0','0','portal:wrongQuestion:list','#','admin','2026-08-19 18:01:46','',NULL,'错题本只读列表','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('知识中心',5000,2,'knowledge-center','ai/knowledge-center/index',NULL,'',1,0,'M','0','0','','documentation','admin','2026-08-19 18:01:46','',NULL,'知识中心目录（知识库管理+知识文库 Tab）','0'),
                                                                                                                                                                                                                        ('AI基础配置',5000,4,'ai-config',NULL,NULL,'',1,0,'M','0','0','','system','admin','2026-08-19 18:01:46','',NULL,'AI基础配置目录（模型配置+工具管理+数据源管理）','0'),
                                                                                                                                                                                                                        ('运营监控',5000,9,'ai-monitor',NULL,NULL,'',1,0,'M','0','0','','monitor','admin','2026-08-19 18:01:46','',NULL,'运营监控目录（概览大屏+Token统计）','0'),
                                                                                                                                                                                                                        ('门户管理',0,2,'portal',NULL,NULL,'',1,0,'M','0','0','','job','admin','2026-08-19 18:01:46','admin','2026-08-20 14:04:35','V10.5: 门户管理一级目录（聚合内容/用户/审核/面试/学习）','0'),
                                                                                                                                                                                                                        ('审核中心',5241,3,'audit-center','cms/audit-center/index',NULL,'',1,0,'C','0','0','system:auditTask:list','eye-open','admin','2026-08-19 18:01:46','admin','2026-08-20 14:05:25','V10.5: 审核中心（统一审核入口，Tab 容器，权限 system:auditTask:*）','0'),
                                                                                                                                                                                                                        ('基础管理',1,1,'base',NULL,NULL,'',1,0,'M','0','0','','tree','admin','2026-08-19 18:01:46','admin','2026-08-21 00:00:00','V10.6: 系统设置-基础管理（用户/角色/菜单/部门/岗位）','0'),
                                                                                                                                                                                                                        ('配置管理',1,2,'/systemConfig',NULL,NULL,'',1,0,'M','0','0','','edit','admin','2026-08-19 18:01:46','admin','2026-09-07 11:37:15','V10.6: 系统设置-配置管理（字典/参数/敏感词）','0'),
                                                                                                                                                                                                                        ('消息通知',1,4,'notice',NULL,NULL,'',1,0,'M','0','0','','message','admin','2026-08-19 18:01:46','admin','2026-08-21 00:00:00','V10.6: 系统设置-消息通知（消息中心/通知管理）','0'),
                                                                                                                                                                                                                        ('会话列表',5001,11,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:list','#','admin','2026-08-24 00:00:00','',NULL,'AI会话列表接口权限','0'),
                                                                                                                                                                                                                        ('会话查询',5001,12,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:query','#','admin','2026-08-24 00:00:00','',NULL,'AI会话历史消息接口权限','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('会话新增',5001,13,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:add','#','admin','2026-08-24 00:00:00','',NULL,'AI会话创建接口权限','0'),
                                                                                                                                                                                                                        ('会话修改',5001,14,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:edit','#','admin','2026-08-24 00:00:00','',NULL,'AI会话标题修改接口权限','0'),
                                                                                                                                                                                                                        ('会话删除',5001,15,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:remove','#','admin','2026-08-24 00:00:00','',NULL,'AI会话删除接口权限','0'),
                                                                                                                                                                                                                        ('语音面试',5192,4,'voiceInterview','cms/voiceInterview/index',NULL,'',1,0,'C','0','0','cms:voiceInterview:list','microphone','admin','2026-08-31 09:12:21','',NULL,'AI语音面试会话管理：列表/详情/评分报告查看','0'),
                                                                                                                                                                                                                        ('语音面试查询',5253,1,'',NULL,NULL,'',1,0,'F','0','0','cms:voiceInterview:query','#','admin','2026-08-31 09:12:21','',NULL,'','0'),
                                                                                                                                                                                                                        ('语音面试删除',5253,2,'',NULL,NULL,'',1,0,'F','0','0','cms:voiceInterview:remove','#','admin','2026-08-31 09:12:21','',NULL,'','0'),
                                                                                                                                                                                                                        ('语音面试导出',5253,3,'',NULL,NULL,'',1,0,'F','0','0','cms:voiceInterview:export','#','admin','2026-08-31 09:12:21','',NULL,'','0'),
                                                                                                                                                                                                                        ('支付管理',0,7,'pay',NULL,NULL,'',1,0,'M','0','0','','money','admin','2026-08-28 09:24:22','',NULL,'V11.0 公共支付通道','0'),
                                                                                                                                                                                                                        ('支付订单',5300,1,'order','cms/pay/order/index',NULL,'',1,0,'C','0','0','cms:payOrder:list','list','admin','2026-08-28 09:24:22','',NULL,'支付单查询/详情/关单','0'),
                                                                                                                                                                                                                        ('分账流水',5300,2,'ledger','cms/pay/ledger/index',NULL,'',1,0,'C','0','0','cms:payLedger:list','money','admin','2026-08-28 09:24:22','',NULL,'平台抽成/用户所得复式记账','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('用户银行卡',5300,3,'bankcard','cms/pay/bankcard/index',NULL,'',1,0,'C','0','0','cms:payBankCard:list','card','admin','2026-08-28 09:24:22','',NULL,'脱敏审计视角','0'),
                                                                                                                                                                                                                        ('支付配置',5300,4,'pay-config','cms/pay/config/index',NULL,'',1,0,'C','0','0','cms:payConfig:view','edit','admin','2026-08-28 09:24:23','',NULL,'通道状态/费率在线调整','0'),
                                                                                                                                                                                                                        ('支付订单查询',5301,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payOrder:query','#','admin','2026-08-28 09:24:22','',NULL,'','0'),
                                                                                                                                                                                                                        ('支付订单关单',5301,2,'',NULL,NULL,'',1,0,'F','0','0','cms:payOrder:close','#','admin','2026-08-28 09:24:22','',NULL,'','0'),
                                                                                                                                                                                                                        ('银行卡详情',5303,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payBankCard:query','#','admin','2026-08-28 09:24:23','',NULL,'','0'),
                                                                                                                                                                                                                        ('费率调整',5304,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payConfig:edit','#','admin','2026-08-28 09:24:23','',NULL,'','0'),
                                                                                                                                                                                                                        ('分账明细',5302,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payLedger:query','#','admin','2026-08-28 09:24:22','',NULL,'单笔支付单分账明细','0'),
                                                                                                                                                                                                                        ('分账汇总',5302,2,'',NULL,NULL,'',1,0,'F','0','0','cms:payLedger:summary','#','admin','2026-08-28 09:24:22','',NULL,'平台抽成/用户所得汇总卡片','0'),
                                                                                                                                                                                                                        ('记账管理',0,8,'ledger',NULL,NULL,'',1,0,'M','0','0','','bookkeeping','admin','2026-09-03 10:57:30','',NULL,'记账模块（个人资产管理）','0'),
                                                                                                                                                                                                                        ('预设分类',5400,1,'category','cms/ledger/category/index',NULL,'',1,0,'C','0','0','cms:ledgerCategory:list','tree','admin','2026-09-03 10:57:30','',NULL,'系统预设收支分类维护','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('运营统计',5400,2,'stats','cms/ledger/stats/index',NULL,'',1,0,'C','0','0','cms:ledgerStats:list','chart','admin','2026-09-03 10:57:31','',NULL,'记账用户/活跃度/类型分布（脱敏聚合）','0'),
                                                                                                                                                                                                                        ('分类查询',5401,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ledgerCategory:query','#','admin','2026-09-03 10:57:30','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类新增',5401,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ledgerCategory:add','#','admin','2026-09-03 10:57:30','',NULL,'','0'),
                                                                                                                                                                                                                        ('分类修改',5401,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ledgerCategory:edit','#','admin','2026-09-03 10:57:30','',NULL,'','0'),
                                                                                                                                                                                                                        ('提供商管理',5238,4,'provider','ai/provider/index',NULL,'',1,0,'C','0','0','cms:ai:model-config:list','server','admin','2026-09-07 09:15:27','',NULL,'AI提供商注册表（V11.0.2 配置驱动）','0'),
                                                                                                                                                                                                                        ('场景配置',5238,10,'scene','ai/scene/index',NULL,'',1,0,'C','0','0','cms:ai:scene:list','component','admin','2026-09-07 15:07:32','',NULL,'AI场景配置中心：业务场景与Agent/模型/知识库/工作流动态绑定（版本+灰度权重）','0'),
                                                                                                                                                                                                                        ('场景查询',5450,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:scene:query','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('场景新增',5450,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:scene:create','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('场景修改',5450,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:scene:update','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('场景删除',5450,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:scene:remove','#','admin','2026-09-07 15:07:32','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('岗位模板',5192,5,'jobTemplate','cms/interview/jobTemplate/index',NULL,'',1,0,'C','0','0','cms:interview:jobTemplate:list','dict','admin','2026-09-07 15:07:32','',NULL,'岗位模板管理：JD/关键词（LLM提取）/出题权重/关联题目','0'),
                                                                                                                                                                                                                        ('岗位模板查询',5455,1,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:jobTemplate:query','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('岗位模板新增',5455,2,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:jobTemplate:create','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('岗位模板修改',5455,3,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:jobTemplate:update','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('岗位模板删除',5455,4,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:jobTemplate:remove','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('面试配置',5192,6,'interviewConfig','cms/interview/interviewConfig/index',NULL,'',1,0,'C','0','0','cms:interview:config:list','edit','admin','2026-09-07 15:07:32','',NULL,'面试配置管理：人设/提示词模板/评分权重/追问策略/自我介绍环节','0'),
                                                                                                                                                                                                                        ('面试配置查询',5460,1,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:config:query','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('面试配置新增',5460,2,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:config:create','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
                                                                                                                                                                                                                        ('面试配置修改',5460,3,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:config:update','#','admin','2026-09-07 15:07:33','',NULL,'','0'),
                                                                                                                                                                                                                        ('面试配置删除',5460,4,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:config:remove','#','admin','2026-09-07 15:07:33','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                                                        ('内容安全检测',5238,11,'safety','ai/safety/index',NULL,'',1,0,'C','0','0','cms:ai:safety:detect','shield','admin','2026-09-11 14:53:02','',NULL,'LLM 级文本敏感内容复核工具（经统一网关 sensitive_word 场景，限流/成本熔断自动生效）','0'),
                                                                                                                                                                                                                        ('文本检测',5470,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:safety:detect','#','admin','2026-09-11 14:53:02','',NULL,'','0'),
                                                                                                                                                                                                                        ('AI执行日志',5238,12,'execute-log','ai/execute-log/index',NULL,'',1,0,'C','0','0','cms:ai:execute-log:list','log','admin','2026-09-11 14:53:08','',NULL,'统一网关全量调用日志：场景/模型/Token/成本/耗时筛选与详情（v11.60 P1-1 可观测性）','0'),
                                                                                                                                                                                                                        ('日志查询',5472,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:execute-log:query','#','admin','2026-09-11 14:53:08','',NULL,'','0'),
                                                                                                                                                                                                                        ('日志删除',5472,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:execute-log:remove','#','admin','2026-09-11 14:53:08','',NULL,'过期数据清理（物理删除，日志只增不改）','0');
INSERT INTO `moyun-db`.sys_notification (`type`,title,content,`data`,`scope`,user_id,user_type,notice_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                   ('todo','新文章待审核：22222222222222222222222222','作者 zhangsan 提交了文章《22222222222222222222222222》，请尽快审核','{"id": 1, "bizType": "article"}','user',1,'sys','1','1','','2026-08-28 09:58:10','','2026-08-28 09:58:30',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：22222222222222222222222222','作者 zhangsan 提交了文章《22222222222222222222222222》，请尽快审核','{"id": 1, "bizType": "article"}','user',2,'sys','1','1','','2026-08-28 09:58:10','','2026-08-28 09:58:30',NULL,'0'),
                                                                                                                                                                                   ('system','文章审核通过：22222222222222222222222222','您的文章《22222222222222222222222222》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 1, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-08-28 09:58:30','','2026-08-28 09:58:30',NULL,'0'),
                                                                                                                                                                                   ('todo','新创作者认证申请待审核','用户 李白（userId=7）提交了创作者认证申请，请尽快审核','{"id": 1, "bizType": "creator_certification"}','user',1,'sys','1','1','','2026-08-28 10:03:46','','2026-08-28 10:04:18',NULL,'0'),
                                                                                                                                                                                   ('todo','新创作者认证申请待审核','用户 李白（userId=7）提交了创作者认证申请，请尽快审核','{"id": 1, "bizType": "creator_certification"}','user',2,'sys','1','1','','2026-08-28 10:03:46','','2026-08-28 10:04:18',NULL,'0'),
                                                                                                                                                                                   ('system','创作者认证已通过','恭喜您，您的创作者认证申请已通过审核，现已获得创作者标识。','{"id": 1, "status": "approved", "bizType": "creator_certification"}','user',7,'portal','1','0','','2026-08-28 10:04:19','','2026-08-28 10:04:19',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：李白文章解析','作者 libai 提交了文章《李白文章解析》，请尽快审核','{"id": 13, "bizType": "article"}','user',1,'sys','1','1','','2026-08-28 13:12:23','','2026-08-28 17:55:00',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：李白文章解析','作者 libai 提交了文章《李白文章解析》，请尽快审核','{"id": 13, "bizType": "article"}','user',2,'sys','1','1','','2026-08-28 13:12:23','','2026-08-28 17:55:00',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：李白文章解析','作者 libai 提交了文章《李白文章解析》，请尽快审核','{"id": 13, "bizType": "article"}','user',8,'portal','1','1','','2026-08-28 13:12:23','','2026-08-28 17:55:00',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：web 端如何实现一个语音转文字的','作者 用户#null 提交了文章《web 端如何实现一个语音转文字的》，请尽快审核','{"id": 7, "bizType": "article"}','user',1,'sys','1','1','','2026-08-28 13:12:39','','2026-08-28 17:54:44',NULL,'0');
INSERT INTO `moyun-db`.sys_notification (`type`,title,content,`data`,`scope`,user_id,user_type,notice_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                   ('todo','新文章待审核：web 端如何实现一个语音转文字的','作者 用户#null 提交了文章《web 端如何实现一个语音转文字的》，请尽快审核','{"id": 7, "bizType": "article"}','user',2,'sys','1','1','','2026-08-28 13:12:39','','2026-08-28 17:54:44',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：web 端如何实现一个语音转文字的','作者 用户#null 提交了文章《web 端如何实现一个语音转文字的》，请尽快审核','{"id": 7, "bizType": "article"}','user',8,'portal','1','1','','2026-08-28 13:12:39','','2026-08-28 17:54:44',NULL,'0'),
                                                                                                                                                                                   ('bookmark','zhangsan 收藏了你的文章','zhangsan 收藏了你的文章《视频渲染服务器配置评估》','{"bizType": "bookmark", "articleId": 2, "fromUserId": 6}','user',7,'portal','1','0','','2026-08-28 13:53:47','','2026-08-28 13:53:46',NULL,'0'),
                                                                                                                                                                                   ('like','zhangsan 赞了你的文章','zhangsan 赞了你的文章《视频渲染服务器配置评估》','{"bizType": "like", "articleId": 2, "fromUserId": 6}','user',7,'portal','1','0','','2026-08-28 13:53:48','','2026-08-28 13:53:47',NULL,'0'),
                                                                                                                                                                                   ('comment','zhangsan 评论了你的文章','zhangsan 评论了你的文章《本企业是否属小型企业或微型企业： 是和否的区别》','{"bizType": "comment", "articleId": 9, "fromUserId": 6}','user',7,'portal','1','0','','2026-08-28 16:02:10','','2026-08-28 16:02:09',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：32242432','作者 zhangsan 提交了文章《32242432》，请尽快审核','{"id": 16, "bizType": "article"}','user',1,'sys','1','1','','2026-08-28 17:50:28','','2026-08-28 17:57:59',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：32242432','作者 zhangsan 提交了文章《32242432》，请尽快审核','{"id": 16, "bizType": "article"}','user',2,'sys','1','1','','2026-08-28 17:50:28','','2026-08-28 17:57:59',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：32242432','作者 zhangsan 提交了文章《32242432》，请尽快审核','{"id": 16, "bizType": "article"}','user',8,'portal','1','1','','2026-08-28 17:50:28','','2026-08-28 17:57:59',NULL,'0'),
                                                                                                                                                                                   ('system','文章审核通过：web 端如何实现一个语音转文字的','您的文章《web 端如何实现一个语音转文字的》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 7, "status": "published", "bizType": "article"}','user',7,'portal','1','0','','2026-08-28 17:54:45','','2026-08-28 17:54:44',NULL,'0'),
                                                                                                                                                                                   ('system','文章审核通过：李白文章解析','您的文章《李白文章解析》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 13, "status": "published", "bizType": "article"}','user',7,'portal','1','0','','2026-08-28 17:55:00','','2026-08-28 17:55:00',NULL,'0');
INSERT INTO `moyun-db`.sys_notification (`type`,title,content,`data`,`scope`,user_id,user_type,notice_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                   ('system','文章审核通过：32242432','您的文章《32242432》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 16, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-08-28 17:58:00','','2026-08-28 17:57:59',NULL,'0'),
                                                                                                                                                                                   ('notice','面经审核结果','您的面经《java面试》审核通过，原因：好的',NULL,'user',6,'portal',NULL,'0','','2026-08-31 10:42:46','','2026-08-31 10:42:46',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：22222222','作者 用户#null 提交了文章《22222222》，请尽快审核','{"id": 14, "bizType": "article"}','user',1,'sys','1','1','','2026-08-31 16:22:50','','2026-08-31 16:23:19',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：22222222','作者 用户#null 提交了文章《22222222》，请尽快审核','{"id": 14, "bizType": "article"}','user',2,'sys','1','1','','2026-08-31 16:22:50','','2026-08-31 16:23:19',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：22222222','作者 用户#null 提交了文章《22222222》，请尽快审核','{"id": 14, "bizType": "article"}','user',8,'portal','1','1','','2026-08-31 16:22:50','','2026-08-31 16:23:19',NULL,'0'),
                                                                                                                                                                                   ('system','文章审核通过：22222222','您的文章《22222222》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 14, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-08-31 16:23:19','','2026-08-31 16:23:19',NULL,'0'),
                                                                                                                                                                                   ('bookmark',' 收藏了你的文章',' 收藏了你的文章《岁月是一场无声的雪》','{"bizType": "bookmark", "articleId": 5, "fromUserId": 6}','user',7,'portal','1','0','','2026-09-02 14:22:40','','2026-09-02 14:22:39',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：处暑过后，傍晚的风开始凉了','作者 zhangsan 提交了文章《处暑过后，傍晚的风开始凉了》，请尽快审核','{"id": 17, "bizType": "article"}','user',1,'sys','1','1','','2026-09-07 10:03:07','','2026-09-07 10:05:37',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：处暑过后，傍晚的风开始凉了','作者 zhangsan 提交了文章《处暑过后，傍晚的风开始凉了》，请尽快审核','{"id": 17, "bizType": "article"}','user',2,'sys','1','1','','2026-09-07 10:03:07','','2026-09-07 10:05:37',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：处暑过后，傍晚的风开始凉了','作者 zhangsan 提交了文章《处暑过后，傍晚的风开始凉了》，请尽快审核','{"id": 17, "bizType": "article"}','user',8,'portal','1','1','','2026-09-07 10:03:07','','2026-09-07 10:05:37',NULL,'0');
INSERT INTO `moyun-db`.sys_notification (`type`,title,content,`data`,`scope`,user_id,user_type,notice_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                                                                   ('system','文章审核通过：处暑过后，傍晚的风开始凉了','您的文章《处暑过后，傍晚的风开始凉了》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 17, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-09-07 10:05:37','','2026-09-07 10:05:37',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：手机端发起的第一个文章','作者 zhangsan 提交了文章《手机端发起的第一个文章》，请尽快审核','{"id": 18, "bizType": "article"}','user',1,'sys','1','1','','2026-09-07 10:15:28','','2026-09-07 10:15:49',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：手机端发起的第一个文章','作者 zhangsan 提交了文章《手机端发起的第一个文章》，请尽快审核','{"id": 18, "bizType": "article"}','user',2,'sys','1','1','','2026-09-07 10:15:28','','2026-09-07 10:15:49',NULL,'0'),
                                                                                                                                                                                   ('todo','新文章待审核：手机端发起的第一个文章','作者 zhangsan 提交了文章《手机端发起的第一个文章》，请尽快审核','{"id": 18, "bizType": "article"}','user',8,'portal','1','1','','2026-09-07 10:15:28','','2026-09-07 10:15:49',NULL,'0'),
                                                                                                                                                                                   ('system','文章审核通过：手机端发起的第一个文章','您的文章《手机端发起的第一个文章》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 18, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-09-07 10:15:49','','2026-09-07 10:15:49',NULL,'0'),
                                                                                                                                                                                   ('system','话题审核通过：哈哈哈，世界太变态了？','您发起的话题《哈哈哈，世界太变态了？》已通过审核并发布。可在「我的话题」中查看详情。','{"id": 1, "status": "active", "bizType": "topic"}','user',6,'portal','1','0','','2026-09-07 17:52:45','','2026-09-07 17:52:45',NULL,'0');
INSERT INTO `moyun-db`.sys_notification_read (notification_id,user_id,user_type,read_time,create_time) VALUES
                                                                                                           (15,7,'portal','2026-08-28 10:27:36','2026-08-28 10:27:36'),
                                                                                                           (19,1,'sys','2026-08-28 13:13:53','2026-08-28 13:13:53'),
                                                                                                           (16,1,'sys','2026-08-28 13:13:56','2026-08-28 13:13:56'),
                                                                                                           (12,6,'portal','2026-08-28 13:50:29','2026-08-28 13:50:29'),
                                                                                                           (30,6,'portal','2026-08-31 10:13:21','2026-08-31 10:13:21'),
                                                                                                           (31,6,'portal','2026-08-31 10:44:50','2026-08-31 10:44:50'),
                                                                                                           (35,6,'portal','2026-08-31 16:24:48','2026-08-31 16:24:48'),
                                                                                                           (40,6,'portal','2026-09-07 10:13:39','2026-09-07 10:13:39'),
                                                                                                           (44,6,'portal','2026-09-07 11:17:01','2026-09-07 11:17:01'),
                                                                                                           (45,6,'portal','2026-09-07 17:59:32','2026-09-07 17:59:32');
INSERT INTO `moyun-db`.sys_post (post_code,post_name,post_sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                       ('ceo','董事长',1,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
                                                                                                                                       ('se','项目经理',2,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
                                                                                                                                       ('hr','人力资源',3,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
                                                                                                                                       ('user','普通员工',4,'0','admin','2026-08-19 18:01:44','',NULL,'','0');
INSERT INTO `moyun-db`.sys_role (role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                                         ('超级管理员','admin',1,'1',1,1,'0','0','admin','2026-08-19 18:01:44','',NULL,'超级管理员'),
                                                                                                                                                                                         ('普通角色','common',2,'2',1,1,'0','0','admin','2026-08-19 18:01:44','',NULL,'普通角色');
INSERT INTO `moyun-db`.sys_role_dept (role_id,dept_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,100,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
                                                                                                              (2,101,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
                                                                                                              (2,105,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,3,'',NULL,'',NULL,NULL),
                                                                                                              (1,116,'',NULL,'',NULL,NULL),
                                                                                                              (1,1055,'',NULL,'',NULL,NULL),
                                                                                                              (1,1056,'',NULL,'',NULL,NULL),
                                                                                                              (1,1057,'',NULL,'',NULL,NULL),
                                                                                                              (1,1058,'',NULL,'',NULL,NULL),
                                                                                                              (1,1059,'',NULL,'',NULL,NULL),
                                                                                                              (1,1060,'',NULL,'',NULL,NULL),
                                                                                                              (1,5000,'',NULL,'',NULL,NULL),
                                                                                                              (1,5001,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5002,'',NULL,'',NULL,NULL),
                                                                                                              (1,5003,'',NULL,'',NULL,NULL),
                                                                                                              (1,5004,'',NULL,'',NULL,NULL),
                                                                                                              (1,5005,'',NULL,'',NULL,NULL),
                                                                                                              (1,5006,'',NULL,'',NULL,NULL),
                                                                                                              (1,5007,'',NULL,'',NULL,NULL),
                                                                                                              (1,5008,'',NULL,'',NULL,NULL),
                                                                                                              (1,5009,'',NULL,'',NULL,NULL),
                                                                                                              (1,5010,'',NULL,'',NULL,NULL),
                                                                                                              (1,5011,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5012,'',NULL,'',NULL,NULL),
                                                                                                              (1,5013,'',NULL,'',NULL,NULL),
                                                                                                              (1,5014,'',NULL,'',NULL,NULL),
                                                                                                              (1,5015,'',NULL,'',NULL,NULL),
                                                                                                              (1,5016,'',NULL,'',NULL,NULL),
                                                                                                              (1,5017,'',NULL,'',NULL,NULL),
                                                                                                              (1,5018,'',NULL,'',NULL,NULL),
                                                                                                              (1,5019,'',NULL,'',NULL,NULL),
                                                                                                              (1,5020,'',NULL,'',NULL,NULL),
                                                                                                              (1,5021,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5022,'',NULL,'',NULL,NULL),
                                                                                                              (1,5023,'',NULL,'',NULL,NULL),
                                                                                                              (1,5024,'',NULL,'',NULL,NULL),
                                                                                                              (1,5025,'',NULL,'',NULL,NULL),
                                                                                                              (1,5026,'',NULL,'',NULL,NULL),
                                                                                                              (1,5027,'',NULL,'',NULL,NULL),
                                                                                                              (1,5028,'',NULL,'',NULL,NULL),
                                                                                                              (1,5029,'',NULL,'',NULL,NULL),
                                                                                                              (1,5030,'',NULL,'',NULL,NULL),
                                                                                                              (1,5031,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5032,'',NULL,'',NULL,NULL),
                                                                                                              (1,5033,'',NULL,'',NULL,NULL),
                                                                                                              (1,5034,'',NULL,'',NULL,NULL),
                                                                                                              (1,5035,'',NULL,'',NULL,NULL),
                                                                                                              (1,5036,'',NULL,'',NULL,NULL),
                                                                                                              (1,5037,'',NULL,'',NULL,NULL),
                                                                                                              (1,5038,'',NULL,'',NULL,NULL),
                                                                                                              (1,5039,'',NULL,'',NULL,NULL),
                                                                                                              (1,5040,'',NULL,'',NULL,NULL),
                                                                                                              (1,5041,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5042,'',NULL,'',NULL,NULL),
                                                                                                              (1,5043,'',NULL,'',NULL,NULL),
                                                                                                              (1,5044,'',NULL,'',NULL,NULL),
                                                                                                              (1,5045,'',NULL,'',NULL,NULL),
                                                                                                              (1,5046,'',NULL,'',NULL,NULL),
                                                                                                              (1,5047,'',NULL,'',NULL,NULL),
                                                                                                              (1,5048,'',NULL,'',NULL,NULL),
                                                                                                              (1,5049,'',NULL,'',NULL,NULL),
                                                                                                              (1,5050,'',NULL,'',NULL,NULL),
                                                                                                              (1,5051,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5052,'',NULL,'',NULL,NULL),
                                                                                                              (1,5053,'',NULL,'',NULL,NULL),
                                                                                                              (1,5054,'',NULL,'',NULL,NULL),
                                                                                                              (1,5055,'',NULL,'',NULL,NULL),
                                                                                                              (1,5056,'',NULL,'',NULL,NULL),
                                                                                                              (1,5057,'',NULL,'',NULL,NULL),
                                                                                                              (1,5058,'',NULL,'',NULL,NULL),
                                                                                                              (1,5059,'admin','2026-08-19 18:01:41','',NULL,NULL),
                                                                                                              (1,5060,'admin','2026-08-19 18:01:41','',NULL,NULL),
                                                                                                              (1,5063,'admin','2026-08-19 18:01:41','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5064,'admin','2026-08-19 18:01:41','',NULL,NULL),
                                                                                                              (1,5065,'admin','2026-08-19 18:01:41','',NULL,NULL),
                                                                                                              (1,5066,'admin','2026-08-19 18:01:41','',NULL,NULL),
                                                                                                              (1,5067,'admin','2026-08-19 18:01:41','',NULL,NULL),
                                                                                                              (1,5068,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5069,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5070,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5071,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5072,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5073,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5074,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5075,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5076,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5077,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5078,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5079,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5080,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5081,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5082,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5083,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5084,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5085,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5086,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5087,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5088,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5089,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5090,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5091,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5092,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5093,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5094,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5095,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5096,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5097,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5098,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5099,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5100,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5101,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5102,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5103,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5104,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5105,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5106,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5107,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5108,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5109,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5110,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5111,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5112,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5113,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5114,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5115,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5116,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5117,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5118,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5119,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5120,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5121,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5122,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5123,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5124,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5125,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5126,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5127,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5128,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5129,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5130,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5134,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5135,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5136,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5137,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5138,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5139,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5140,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5141,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5142,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5143,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (1,5146,'admin','2026-08-25 00:00:00','',NULL,NULL),
                                                                                                              (1,5192,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5193,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5194,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5195,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5196,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5197,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5198,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5199,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5200,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5201,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5202,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5203,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5204,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5205,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5206,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5207,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5208,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5209,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5210,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5211,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5212,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5213,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5214,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5215,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5216,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5217,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5218,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5219,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5220,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5221,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5222,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5223,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5224,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5225,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5226,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5227,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5228,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5229,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5230,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5231,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5232,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5233,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5234,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5235,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5236,'admin','2026-08-19 18:01:46','',NULL,NULL),
                                                                                                              (1,5237,'',NULL,'',NULL,NULL),
                                                                                                              (1,5238,'',NULL,'',NULL,NULL),
                                                                                                              (1,5239,'',NULL,'',NULL,NULL),
                                                                                                              (1,5248,'admin','2026-08-24 00:00:00','',NULL,NULL),
                                                                                                              (1,5249,'admin','2026-08-24 00:00:00','',NULL,NULL),
                                                                                                              (1,5250,'admin','2026-08-24 00:00:00','',NULL,NULL),
                                                                                                              (1,5251,'admin','2026-08-24 00:00:00','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5252,'admin','2026-08-24 00:00:00','',NULL,NULL),
                                                                                                              (1,5300,'',NULL,'',NULL,NULL),
                                                                                                              (1,5301,'',NULL,'',NULL,NULL),
                                                                                                              (1,5302,'',NULL,'',NULL,NULL),
                                                                                                              (1,5303,'',NULL,'',NULL,NULL),
                                                                                                              (1,5304,'',NULL,'',NULL,NULL),
                                                                                                              (1,5311,'',NULL,'',NULL,NULL),
                                                                                                              (1,5312,'',NULL,'',NULL,NULL),
                                                                                                              (1,5313,'',NULL,'',NULL,NULL),
                                                                                                              (1,5314,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5315,'',NULL,'',NULL,NULL),
                                                                                                              (1,5316,'',NULL,'',NULL,NULL),
                                                                                                              (1,5400,'',NULL,'',NULL,NULL),
                                                                                                              (1,5401,'',NULL,'',NULL,NULL),
                                                                                                              (1,5402,'',NULL,'',NULL,NULL),
                                                                                                              (1,5411,'',NULL,'',NULL,NULL),
                                                                                                              (1,5412,'',NULL,'',NULL,NULL),
                                                                                                              (1,5413,'',NULL,'',NULL,NULL),
                                                                                                              (1,5450,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5451,'admin','2026-09-07 15:07:33','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5452,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5453,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5454,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5455,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5456,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5457,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5458,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5459,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5460,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5461,'admin','2026-09-07 15:07:33','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,5462,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5463,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5464,'admin','2026-09-07 15:07:33','',NULL,NULL),
                                                                                                              (1,5470,'',NULL,'',NULL,NULL),
                                                                                                              (1,5471,'',NULL,'',NULL,NULL),
                                                                                                              (1,5472,'',NULL,'',NULL,NULL),
                                                                                                              (1,5473,'',NULL,'',NULL,NULL),
                                                                                                              (1,5474,'',NULL,'',NULL,NULL),
                                                                                                              (2,1,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,2,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,3,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,100,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,101,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,102,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,103,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,104,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,105,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,106,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,108,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,109,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,110,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,111,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,112,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,113,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,114,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,116,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,117,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,500,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,501,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1000,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,1001,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1002,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1003,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1004,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1005,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1006,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1007,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1008,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1009,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1010,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,1011,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1012,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1013,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1014,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1015,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1016,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1017,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1018,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1019,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1020,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,1021,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1022,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1023,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1024,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1025,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1026,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1027,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1028,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1029,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1030,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,1031,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1032,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1033,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1034,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1039,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1040,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1041,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1042,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1043,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1044,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,1045,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1046,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1047,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1048,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1049,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1050,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1051,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1052,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1053,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1054,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (2,1055,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1056,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1057,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1058,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1059,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,1060,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,5134,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,5135,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,5137,'admin','2026-08-19 18:01:45','',NULL,NULL),
                                                                                                              (2,5138,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
    (2,5139,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_sensitive_word (word,category,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
                                                                                                                                 ('示例敏感词1','other','0','admin','2026-08-19 18:01:41',NULL,NULL,'示例词，生产环境请替换为真实词库','0'),
                                                                                                                                 ('示例敏感词2','ad','0','admin','2026-08-19 18:01:41',NULL,NULL,'示例词，生产环境请替换为真实词库','0'),
                                                                                                                                 ('示例-广告','ad','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
                                                                                                                                 ('示例-辱骂','insult','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
                                                                                                                                 ('示例-色情','porn','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
                                                                                                                                 ('示例-政治','politics','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
                                                                                                                                 ('示例-其他','other','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_user (dept_id,user_name,nick_name,user_type,email,phonenumber,sex,avatar,password,status,del_flag,login_ip,login_date,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                                                                                                                         (103,'admin','若依','00','ry@163.com','15888888888','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-09-11 13:32:22','admin','2026-08-19 18:01:44','','2026-09-11 13:32:22','管理员'),
                                                                                                                                                                                                         (105,'ry','若依','00','ry@qq.com','15666666666','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-08-19 18:01:44','admin','2026-08-19 18:01:44','',NULL,'测试员');
INSERT INTO `moyun-db`.sys_user_post (user_id,post_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,1,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
                                                                                                              (2,2,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
INSERT INTO `moyun-db`.sys_user_role (user_id,role_id,create_by,create_time,update_by,update_time,remark) VALUES
                                                                                                              (1,1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
                                                                                                              (2,2,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL);





-- add menu

INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5475, '文件管理', 5243, 6, 'file', 'system/file/index', null, '', 1, 0, 'C', '0', '0', 'system:file:list', 'upload', 'admin', NOW(), '', null, '系统文件管理：MinIO/本地存储的文件列表、上传、预览、下载、删除与存储模式切换', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5476, '文件查询', 5475, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'system:file:query', '#', 'admin', NOW(), '', null, '文件详情查看（GET /system/file/{id}）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5477, '文件上传', 5475, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'system:file:add', '#', 'admin', NOW(), '', null, '文件上传与存储模式切换（POST /system/file/upload、PUT /storage/switch）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5478, '文件删除', 5475, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'system:file:remove', '#', 'admin', NOW(), '', null, '单删/批删/按URL删（删除同时清理存储与记录，不可恢复）', '0');

-- 管理员角色授权
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES (1, 5475), (1, 5476), (1, 5477), (1, 5478);













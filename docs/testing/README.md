# 语曰测试工作区

本目录是测试契约、环境探测和证据索引的唯一入口。  
结论只能从本目录与 `docs/testing/evidence/` 中的机器产物聚合，不能凭聊天记录宣称通过。

状态枚举：`PASS` | `FAIL` | `BLOCKED` | `UNSUPPORTED` | `INCONCLUSIVE`。  
未执行或缺证据不得记为 `PASS`。

| 文件 | 用途 |
|------|------|
| [00-environment-probe.md](./00-environment-probe.md) | 本轮环境指纹与能力探测 |
| [01-test-contract.md](./01-test-contract.md) | MFQ×PPDCS 契约与判定规则 |
| [cases.jsonl](./cases.jsonl) | 机器可读用例（每行一条） |
| [results.jsonl](./results.jsonl) | 执行结果（每行一条，追加写） |
| [defects.md](./defects.md) | 已复现缺陷账本 |
| evidence/ | 命令输出、JUnit、截图、logcat |

当前产品主路径（以代码为准）：

`CaptureKernel → 本机 Room 立即落盘（可选 Domain dual-write）→ FastExtract / StructuredTurn 可选润色`

回忆与问句走本机 FTS / 向量，**不得**经工具写库新建日记。`AgentLoop` / `LocalAgentTools` 已从产品路径移除。

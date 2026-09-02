# Git 提交与推送规则 (Git Workflow Rules)

- **每改一版即时提交 (Commit Per Version/Change)**:
  - 每次完成一版修改、功能特性、Bug 修复或重构优化后，必须及时执行一次 `git commit`。
  - Commit 信息需清晰、准确地概括本次版本的改动内容（遵循语义化提交规范，如 `feat: ...`, `fix: ...`, `refactor: ...`, `docs: ...` 等）。
  - 提交粒度需保持清晰，避免将不相关的改动堆积在同一个 commit 中。

- **推送限制 (Push Restrictions)**:
  - **严禁擅自执行 `git push`**。
  - 只有在用户明确发出推送指令（如“推送到远程”、“可以 push”、“提交到 GitHub/远程仓库”等）后，方可执行 `git push`。

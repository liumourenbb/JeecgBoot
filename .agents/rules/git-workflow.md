# Git 提交与推送规则 (Git Workflow Rules)

- **每改一版即时提交 (Commit Per Version/Change)**:
  - 每次完成一版修改、功能特性、Bug 修复或重构优化后，必须及时执行一次 `git commit`。
  - **提交注释统一写中文 (Commit Messages in Chinese)**：Commit 提交记录的说明文字必须统一使用中文（遵循语义化前缀规范，如 `feat: 新增...`, `fix: 修复...`, `refactor: 重构...`, `docs: 文档...`, `chore: ...` 等，前缀后的描述内容必须使用清晰、准确的中文）。
  - 提交粒度需保持清晰，避免将不相关的改动堆积在同一个 commit 中。

- **推送限制 (Push Restrictions)**:
  - **严禁擅自执行 `git push`**。
  - 只有在用户明确发出推送指令（如“推送到远程”、“可以 push”、“提交到 GitHub/远程仓库”等）后，方可执行 `git push`。

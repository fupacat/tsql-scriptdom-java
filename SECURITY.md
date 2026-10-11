# Security Policy

## Supported Versions

Only the latest release receives security updates and bug fixes.

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |
| < 0.1.0 | :x:                |

## Reporting a Vulnerability

We take the security of `tsql-scriptdom-java` seriously. If you believe you have found a security vulnerability in this project, please follow these guidelines:

### Preferred Method: GitHub Private Vulnerability Reporting
Please report vulnerabilities privately via [GitHub's Advisory tab](https://github.com/fupacat/tsql-scriptdom-java/security/advisories/new). This allows us to assess the issue, coordinate a fix, and publish an advisory without premature public disclosure.

### Direct Contact
If Private Vulnerability Reporting is unavailable, please open a confidential issue or contact the repository maintainer directly via GitHub profile contact information.

### What to Include
When reporting a vulnerability, please provide:
1. A clear description of the potential vulnerability.
2. Steps or sample T-SQL scripts required to reproduce the issue (e.g. potential Denial of Service, stack overflow in AST traversal, or malicious input handling).
3. The affected versions or commits.
4. Any potential mitigations or patch suggestions you may have.

### Response Timeline
- **Initial Acknowledgement:** Within 48 hours.
- **Triage & Status Update:** Within 7 business days.
- **Resolution:** Critical security issues will be patched and released with priority.

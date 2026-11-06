# Third-party notices

The project's personal-use license applies to the project's original code. Bundled third-party libraries retain their own licenses.

- KaTeX 0.16.22: MIT, by the KaTeX contributors. Bundled JavaScript, stylesheets and fonts are in `shared/katex`; the license is included in `shared/katex/LICENSE` and copied into the APK with the library.
- React, Radix UI, React Hook Form, Zod, TanStack Query, Recharts, Tailwind CSS, Lucide and Sonner retain their upstream licenses. The parent panel includes compiled distributions and license files in `admin/assets/notices/`.
- shadcn-admin UI components: MIT, Copyright (c) 2024 Sat Naing. Its license is included in `admin/assets/notices/shadcn-admin.txt`.
- Question bank `data/banks/grade3-ext-thl529*.json`: adapted from thl529/question-bank (https://github.com/thl529/question-bank). The source repository states no license; its author notes the questions are for study reference only, not for commercial use.
- Question banks `data/banks/junior-ext-cjeval-*.json`: adapted from CJEval (https://github.com/SmileWHC/CJEval; Zhang et al., 2024, arXiv:2409.16202). CJEval is intended solely for academic and research purposes; commercial use is prohibited.
- The existing bundled QR-code generator retains the copyright and license notice in its distribution.

The reference APK inspected during interface design is not bundled with this project. No code, images or fonts from it are distributed here.

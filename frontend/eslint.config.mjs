import reactHooks from "eslint-plugin-react-hooks";
import typescriptEslint from "@typescript-eslint/eslint-plugin";
import typescriptParser from "@typescript-eslint/parser";

const eslintConfig = [
  {
    files: ["**/*.{js,jsx,ts,tsx}"],
    languageOptions: {
      parser: typescriptParser,
      parserOptions: {
        ecmaVersion: "latest",
        sourceType: "module",
        ecmaFeatures: {
          jsx: true,
        },
      },
    },
    plugins: {
      "react-hooks": reactHooks,
      "@typescript-eslint": typescriptEslint,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
      "no-console": "error",
    },
  },
  {
    ignores: [
      ".next/**",
      "node_modules/**",
      "out/**",
      "dist/**",
      "build/**",
      ".turbo/**",
    ],
  },
];

export default eslintConfig;

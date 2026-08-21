/// <reference types="vitest/config" />
import { defineConfig } from 'vitest/config';

export default defineConfig({
    test: {
        include: ['test/unit/**/*.spec.ts'],
        environment: 'node'
    }
});

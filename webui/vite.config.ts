import path from 'node:path'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const outRootDir = path.join(__dirname, "dist")

export default defineConfig(() => ({
	plugins: [react()],
    server: {
        host: true,
        port: 3000,
    },
    preview: {
        port: 3000,
    },
	resolve: {
		alias: {
			'@': path.resolve(__dirname, './src')
		}
	},
    publicDir: 'static',
    build: {
        target: 'es6',
        minify: true,
        sourcemap: true,
        outDir: outRootDir,
        emptyOutDir: true
    }
}))

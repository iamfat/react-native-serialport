import typescript from '@rollup/plugin-typescript';

export default [
    {
        input: 'src/index.ts',
        output: {
            format: 'esm',
            dir: './dist',
        },
        plugins: [typescript({ declaration: true, declarationDir: 'dist' })],
        external: ['react-native', 'js-base64'],
    },
];

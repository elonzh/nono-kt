import { defineConfig } from 'vitepress'

const repository = 'nono-kt'
const docsBase = process.env.DOCS_BASE ?? '/'

export default defineConfig({
  title: 'nono-kt',
  description: 'Kotlin and Java client for the nono CLI',
  base: docsBase,
  cleanUrls: true,
  lastUpdated: true,
  srcExclude: ['releasing.md', 'superpowers/**/*.md'],
  locales: {
    root: {
      label: 'English',
      lang: 'en-US',
      title: 'nono-kt',
      description: 'Kotlin and Java client for the nono CLI'
    },
    'zh-Hans': {
      label: '简体中文',
      lang: 'zh-CN',
      link: '/zh-Hans/',
      title: 'nono-kt',
      description: '面向 nono CLI 的 Kotlin 与 Java 客户端',
      themeConfig: {
        nav: [
          { text: '指南', link: '/zh-Hans/getting-started' },
          { text: 'API', link: '/zh-Hans/run-api' },
          { text: '兼容性', link: '/zh-Hans/compatibility' }
        ],
        sidebar: [
          {
            text: '文档',
            items: [
              { text: '入门', link: '/zh-Hans/getting-started' },
              { text: '运行与进程 API', link: '/zh-Hans/run-api' },
              { text: 'Profile 与诊断', link: '/zh-Hans/profiles-and-diagnostics' },
              { text: '兼容性', link: '/zh-Hans/compatibility' }
            ]
          }
        ],
        outline: { label: '本页目录' },
        docFooter: { prev: '上一页', next: '下一页' },
        lastUpdated: { text: '最后更新于' },
        editLink: {
          pattern: `https://github.com/elonzh/${repository}/edit/main/docs/:path`,
          text: '在 GitHub 上编辑此页'
        }
      }
    }
  },
  themeConfig: {
    nav: [
      { text: 'Guide', link: '/getting-started' },
      { text: 'API', link: '/run-api' },
      { text: 'Compatibility', link: '/compatibility' }
    ],
    sidebar: [
      {
        text: 'Documentation',
        items: [
          { text: 'Getting started', link: '/getting-started' },
          { text: 'Run and process API', link: '/run-api' },
          { text: 'Profiles and diagnostics', link: '/profiles-and-diagnostics' },
          { text: 'Compatibility', link: '/compatibility' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: `https://github.com/elonzh/${repository}` }
    ],
    search: { provider: 'local' },
    editLink: {
      pattern: `https://github.com/elonzh/${repository}/edit/main/docs/:path`,
      text: 'Edit this page on GitHub'
    },
    footer: {
      message: 'Released under the Apache License 2.0.',
      copyright: 'Copyright © 2026 elonzh'
    }
  }
})

import { getDirname, path } from "vuepress/utils";
import { hopeTheme } from "vuepress-theme-hope";

import navbar from "./navbar.js";
import sidebar from "./sidebar/index.js";

const __dirname = getDirname(import.meta.url);

export default hopeTheme({
  hostname: "https://www.seven97.top/",
  logo: "/logo.png",
  favicon: "/favicon.ico",

  iconAssets: "//at.alicdn.com/t/c/font_2922463_o9q9dxmps9.css",

  author: {
    name: "Seven",
    url: "https://www.seven97.top/article/",
  },

  repo: "https://github.com/Seven-97/SevenBlog",
  docsDir: "docs",
  // 纯净模式：https://theme-hope.vuejs.press/zh/guide/interface/pure.html
  pure: true,
  breadcrumb: false,
  navbar,
  sidebar,
  footer:
    '<a href="https://beian.miit.gov.cn/" target="_blank">闽ICP备2022017393号</a>',
  displayFooter: true,

  pageInfo: ["Author", "Category", "Tag", "Original", "Word", "ReadingTime"],

  blog: {

    sidebarDisplay: "mobile",
    medias: {
      Github: "https://github.com/Seven-97",
      Gitee: "https://gitee.com/Seven-97",
    },
  },

  plugins: {
    

    blog: true,

    copyright: {
      author: "Seven",
      license: "MIT",
      triggerLength: 100,
      maxLength: 700,
      canonical: "https://www.seven97.top/",
      global: true,
    },

    feed: {
      atom: true,
      json: true,
      rss: true,
    },

    mdEnhance: {
      align: true,
      codetabs: true,
      figure: true,
      gfm: true,
      hint: true,
      include: {
        resolvePath: (file, cwd) => {
          if (file.startsWith("@"))
            return path.resolve(
              __dirname,
              "../snippets",
              file.replace("@", "./"),
            );

          return path.resolve(cwd, file);
        },
      },
      tasklist: true,
    },

    search: {
      isSearchable: (page) => page.path !== "/",
      maxSuggestions: 10,
    },
  },
});

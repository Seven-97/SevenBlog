import { navbar } from "vuepress-theme-hope";

export default navbar([
  { text: "打怪升级", icon: "java", link: "/home.md" },
  { text: "技术书籍", icon: "book", link: "/books/" },
  // {
  //   text: "网站相关",
  //   icon: "about",
  //   children: [
  //     { text: "关于作者", icon: "zuozhe", link: "/about-the-author/" },
  //     {
  //       text: "更新历史",
  //       icon: "history",
  //       link: "/timeline/",
  //     },
  //   ],
  // },
]);

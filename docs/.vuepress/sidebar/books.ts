import { arraySidebar } from "vuepress-theme-hope";

export const books = arraySidebar([
  
  {
    text: "软件质量",
    icon: "highavailable",
    prefix: "software-quality/",
    collapsible: true,
    children: [
      "alibaba-developmentmanual",
      "effectivejava-summary",
      
    ]
  }

]);

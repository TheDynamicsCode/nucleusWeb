/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.nucleusweb.server;

/**
 *
 * @author Dominik
 */
public class HtmlScripts {
   
      
    public static final String EXEC = new StringBuilder().append("<script>\n")
            .append("function exec(evt, group) {\n")
            .append("  var i, tabcontent, tablinks;\n")
            .append("  tabcontent = document.getElementsByClassName(\"tabcontent\");\n")
            .append("  for (i = 0; i < tabcontent.length; i++) {\n")
            .append("    tabcontent[i].style.display = \"none\";\n")
            .append("  }\n")
            .append("  tablinks = document.getElementsByClassName(\"tablinks\");\n")
            .append("  for (i = 0; i < tablinks.length; i++) {\n")
            .append("    tablinks[i].className = tablinks[i].className.replace(\" active\", \"\");\n")
            .append("  }\n")
            .append("  document.getElementById(group).style.display = \"block\";\n")
            .append("  evt.currentTarget.className += \" active\";\n" )
            .append("}\n" )
            .append("// Get the element with id=\"defaultOpen\" and click on it\n" )
            .append("document.getElementById(\"defaultOpen\").click();\n" )
            .append("</script>").toString();
    
     public static final String SEARCH_IN_TABLE = "<script>\n" +
            "function search(tabindx) {\n" +
            "  var input, filter, filterTab, table, tr, td, i, j, txtValue, helper;\n" +
            "  input = document.getElementById(\"searchInput.\"+tabindx);\n" +
            "  filter = input.value.toUpperCase();\n" +
            "  filterTab = filter.split(' ');\n" +
            "  table = document.getElementById(\"commandTab.\"+tabindx);\n" +
            "  tr = table.getElementsByTagName(\"tr\");\n" +
            "  for (i = 0; i < tr.length; i++) {\n" +
            "    td = tr[i].getElementsByTagName(\"td\")[1];\n" +
            "    if (td) {\n" +
            "      txtValue = td.textContent || td.innerText;\n" +
            "      helper=0;\n" +
            "      for (j = 0;  j< filterTab.length; j++) {\n" +
            "              if (txtValue.toUpperCase().indexOf(filterTab[j]) > -1) {\n" +
            "                helper++;\n" +
            "              }        \n" +
            "      }\n" +
            "      \n" +
            "      if (filterTab.length<=helper) {\n" +
            "           tr[i].style.display = \"\";\n" +
            "      } else {\n" +
            "           tr[i].style.display = \"none\";\n" +
            "      }\n" +
            "     \n" +
            "    } \n" +
            "  }\n" +
            "}\n" +
            "</script>";
    /*
    public static final String SEARCH_IN_TABLE = "<script>\n" +
            "function search(tabindx) {\n" +
            "  var input, filter, table, tr, td, i, txtValue;\n" +
            "  input = document.getElementById(\"searchInput.\"+tabindx);\n" +
            "  filter = input.value.toUpperCase();\n" +
            "  table = document.getElementById(\"commandTab.\"+tabindx);\n" +
            "  tr = table.getElementsByTagName(\"tr\");\n" +
            "  for (i = 0; i < tr.length; i++) {\n" +
            "    td = tr[i].getElementsByTagName(\"td\")[0];\n" +
            "    if (td) {\n" +
            "      txtValue = td.textContent || td.innerText;\n" +
            "      if (txtValue.toUpperCase().indexOf(filter) > -1) {\n" +
            "        tr[i].style.display = \"\";\n" +
            "      } else {\n" +
            "        tr[i].style.display = \"none\";\n" +
            "      }\n" +
            "    }       \n" +
            "  }\n" +
            "}\n" +
            "</script>";
   */
    
           
    
}

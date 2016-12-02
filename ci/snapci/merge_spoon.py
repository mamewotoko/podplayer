from lxml import html
import sys, os

report_files = sys.argv[1:]

head_element = None
merged_html = html.fromstring("<div></div>")

hero_css = """
<style>
.hero-unit h1 { font-size: 16px; margin: 2px; }
.hero-unit p { font-size: 14px }
.hero-unit { margin-bottom: 0px; padding: 2px; }
.container { margin-bottom: 10px; }
</style>
"""

for report in report_files:
    with open(report, "r") as f:
        s = f.read()
    testname = os.path.basename(os.path.dirname(os.path.dirname(report)))
    h = html.fromstring(s)
    if head_element is None:
        head_element = h.head
        head_element.append(html.fromstring(hero_css))
    container_elements = h.xpath("//div[@class='container']")
    for cont in container_elements:
        testtitle = cont.xpath("//div[@class='hero-unit']/h1")[0]
        original_report = html.fromstring('<a href="%s">%s</a>' % (report, testname))
        testtitle.clear()
        testtitle.append(original_report)
        merged_html.append(cont)

root = html.fromstring("<html></html>")
root.append(head_element)
root.append(merged_html)

print html.tostring(root)


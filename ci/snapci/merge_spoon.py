from lxml import html
import sys, os

report_files = sys.argv[1:]

head_element = None
merged_html = html.fromstring("<div></div>")

hero_css = """
<style type="text/css">
body { padding-top: 20px; padding-left: 5px; padding-right: 5px }
.hero-unit h1 { font-size: 16px; margin: 2px; }
.hero-unit p { font-size: 14px }
.hero-unit { margin-bottom: 0px; padding: 2px; }
.container { margin: 5px;}
.birds-eye .device th { display: none }
</style>
"""

footer_html = """
<script type="text/javascript">
$('.test a').popover({
    placement: 'top',
    trigger: 'hover'
});
</script>
"""

for report in sorted(report_files):
    with open(report, "r") as f:
        s = f.read()
    report_dir = os.path.dirname(report)
    testname = os.path.basename(os.path.dirname(report_dir))
    h = html.fromstring(s)
    if head_element is None:
        head_element = h.head
        head_element.append(html.fromstring(hero_css))
    container_elements = h.xpath("//div[@class='container']")
   
    for cont in container_elements:
        for a_element in cont.xpath("//a"):
            ## TODO: if href is relative
            newpath = os.path.join(report_dir, a_element.attrib['href'])
            a_element.attrib['href'] = newpath
        testtitle = cont.xpath("//div[@class='hero-unit']/h1")[0]
        original_report = html.fromstring('<a href="%s">%s</a>' % (report, testname))
        testtitle.clear()
        testtitle.append(original_report)
        merged_html.append(cont)

root = html.fromstring("<html></html>")
root.append(head_element)
root.append(merged_html)
root.append(html.fromstring(footer_html))

print html.tostring(root)

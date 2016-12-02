from lxml import html
import sys

report_files = sys.argv[1:]

head_element = None
merged_html = html.fromstring("<div></div>")

for report in report_files:
    with open(report, "r") as f:
        s = f.read()
    
    h = html.fromstring(s)
    if head_element is None:
        head_element = h.head
        head_element.append(html.fromstring("<style>.hero-unit h1 {font-size: 24px} .hero-unit { margin-bottom: 0px; padding: 5px;} .container {margin-bottom: 10px;} </style>"))
    merged_html.append(h.xpath("//div[@class='container']")[0])

root = html.fromstring("<html></html>")
root.append(head_element)
root.append(merged_html)

print html.tostring(root)


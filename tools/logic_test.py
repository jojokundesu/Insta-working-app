# Offline mirror-test of the Android parse/detection rules (same regexes as Engine.java)
import re
def parse_username(s):
    s=re.sub(r'(?i)^https?://(www\.)?instagram\.com/','',s.strip()); s=re.sub(r'(?i)^instagram\.com/','',s)
    s=re.split(r'[/?#]',s)[0].replace('@','')
    return s if re.fullmatch(r'[A-Za-z0-9._]{1,30}',s) else ''
assert parse_username("https://www.instagram.com/nasa/?hl=en")=="nasa"
assert parse_username("@some.user_1")=="some.user_1"
assert parse_username("bad handle!")==""
html='<meta property="og:description" content="12.3M Followers, 100 Following, 456 Posts - x">'
assert re.search(r'([\d,.]+[KMB]?) Followers, ([\d,.]+[KMB]?) Following, ([\d,.]+[KMB]?) Posts',html).groups()==("12.3M","100","456")
assert re.findall(r"#(\w+)","love #Texas #road_trips")==["Texas","road_trips"]
print("logic tests OK")

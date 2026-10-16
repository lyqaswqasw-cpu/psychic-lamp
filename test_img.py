import urllib.request

prefixes = [
    "https://img.ysscores.com/news/",
    "https://news-ar.ysscores.com/storage/news/",
    "https://news-ar.ysscores.com/uploads/news/",
    "https://news-ar.ysscores.com/images/news/",
    "https://api.ysscores.com/uploads/news/",
    "https://ysscores.com/images/news/"
]

img = "17879568549763.jpg"

for p in prefixes:
    url = p + img
    try:
        req = urllib.request.Request(url, method="HEAD")
        resp = urllib.request.urlopen(req, timeout=3)
        print(f"Found: {url} -> {resp.status}")
    except Exception as e:
        print(f"Failed: {url} -> {e}")

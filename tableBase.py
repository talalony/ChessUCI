import sys
sys.path.append("c:\\users\\tal\\appdata\\local\\programs\\python\\python39\\lib\\site-packages")
import requests
from bs4 import BeautifulSoup
import json


color = sys.argv[1]

url = "https://syzygy-tables.info/?fen="
fen = sys.argv[2].replace(" ", "_")
page = requests.get(url+fen)
soup = BeautifulSoup(page.content, 'html.parser')
win = str(soup.find("h2", {"id": "status"}).contents[0])
isWinning = False
draw = False
toWin = "White is winning"
if color == "false":
    toWin = "Black is winning"
if toWin in win:
    isWinning = True
elif "draw" in win.lower():
    draw = True

status = ""
if draw:
    status = "drawing"
elif isWinning:
    status = "winning"
else:
    status = "losing"

i = 0
s = status
while True:
    if i == 1:
        s = "cursed"
    elif i == 2:
        s = "blessed"
    elif i == 3:
        break
    try:
        move = soup.find("div", {"id": s}).contents[0].text.split(" ")[0]
        break
    except:
        i += 1
        continue

dic = {
    "status": status,
    "move": move
}

json_object = json.dumps(dic, indent=2)

# Writing to sample.json
with open("sample.json", "w") as outfile:
    outfile.write(json_object)
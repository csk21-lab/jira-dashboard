import requests
try:
    r = requests.get('https://abc.com', verify=False)
    print(r.status_code)
except Exception as e:
    print("Connection error:", e)

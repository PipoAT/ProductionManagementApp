import http.server
import socketserver
import os
import json
import threading
from tkinter import Tk, Button

class CustomHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/list':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            files = os.listdir('.')
            self.wfile.write(json.dumps(files).encode())
        else:
            super().do_GET()

# change the working directory here
os.chdir('T:\\APIPO\\DOCTEST')

# create a simple HTTP server
httpd = socketserver.TCPServer(("", 1025), CustomHandler)

def start_server():
    print("Server started at localhost:1025")
    threading.Thread(target=httpd.serve_forever).start()

def stop_server():
    print("Server stopped")
    httpd.shutdown()

# create a tkinter window
root = Tk()

# create start and stop buttons
start_button = Button(root, text="Start Server", command=start_server)
stop_button = Button(root, text="Stop Server", command=stop_server)

# pack the buttons
start_button.pack()
stop_button.pack()

# start the tkinter main loop
root.mainloop()

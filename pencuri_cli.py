import requests
from bs4 import BeautifulSoup
import sys
import re
import logging
import os
import platform
import webbrowser
import urllib3

# --- CONFIGURATION ---
BASE_URL = "https://ww93.pencurimovie.bond"
LOG_FILE = "scraper_debug.log"
EXTENSION_FILENAME = "ublock.crx"

# Headers
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36",
    "Referer": BASE_URL,
    "Upgrade-Insecure-Requests": "1",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5"
}

# Disable SSL Warnings
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class PencuriMovieCLI:
    def __init__(self):
        self.setup_logging()
        self.session = requests.Session()
        self.session.headers.update(HEADERS)
        self.session.verify = False 
        self.os_type = platform.system()

    def setup_logging(self):
        if os.path.exists(LOG_FILE):
            try: os.remove(LOG_FILE)
            except: pass
        logging.basicConfig(
            filename=LOG_FILE,
            level=logging.DEBUG,
            format='%(asctime)s - %(levelname)s - %(message)s',
            encoding='utf-8'
        )
        self.logger = logging.getLogger()

    def get_soup(self, url):
        try:
            res = self.session.get(url, timeout=15, verify=False)
            if res.status_code != 200: return None
            soup = BeautifulSoup(res.text, 'html.parser')
            
            # Cloudflare Check
            if "Just a moment" in (soup.title.string if soup.title else ""):
                print("\n[!] Blocked by Cloudflare. Cookies required.")
                return None
            return soup
        except Exception as e:
            print(f"[!] Connection Error: {e}")
            return None

    # --- PLAYBACK ENGINE (HYBRID) ---
    def play_video(self, url):
        print(f"\n[>] Preparing to play: {url}")
        
        # MODE A: WINDOWS (Use Selenium + Edge + uBlock)
        if self.os_type == "Windows":
            try:
                from selenium import webdriver
                from selenium.webdriver.edge.options import Options as EdgeOptions
                from selenium.webdriver.edge.service import Service as EdgeService
                from webdriver_manager.microsoft import EdgeChromiumDriverManager
                
                print("[*] Windows detected: Launching Edge with AdBlock...")
                options = EdgeOptions()
                
                # Load uBlock if available
                if os.path.exists(EXTENSION_FILENAME):
                    options.add_extension(os.path.abspath(EXTENSION_FILENAME))
                    print("[+] uBlock Origin Loaded!")
                else:
                    print(f"[!] Warning: {EXTENSION_FILENAME} not found. Ads will appear.")

                options.add_experimental_option("detach", True)
                options.add_experimental_option("excludeSwitches", ["enable-automation"])
                options.add_experimental_option('useAutomationExtension', False)

                service = EdgeService(EdgeChromiumDriverManager().install())
                driver = webdriver.Edge(service=service, options=options)
                driver.get(url)
                print("[>] Player launched in Edge.")
                return
            except Exception as e:
                print(f"[!] Selenium failed ({e}). Falling back to standard browser.")

        # MODE B: ANDROID / LINUX / FALLBACK (Use Default Browser)
        # This is what will run on your Android phone
        print(f"[*] System ({self.os_type}): Opening default browser...")
        print("[i] On Android, this opens your Chrome/Samsung Internet/VLC")
        webbrowser.open(url)

    # --- 1. SEARCH ---
    def search(self, query):
        return self.parse_listing(f"{BASE_URL}/?s={query.replace(' ', '+')}")

    # --- 2. PARSE LISTINGS ---
    def parse_listing(self, url):
        soup = self.get_soup(url)
        if not soup: return []

        results = []
        items = soup.find_all("div", class_="ml-item")
        if not items: items = soup.find_all("article")

        for item in items:
            title_tag = item.find("h2")
            link_tag = item.find("a")
            
            if title_tag and link_tag:
                title = title_tag.get_text(strip=True)
                link = link_tag['href']
                if title.upper() in ["WEB-DL", "HD", "CAM"]: continue

                type_lbl = "SERIES" if ("/series/" in link or "season" in title.lower()) else "MOVIE"
                
                results.append({
                    "id": len(results) + 1,
                    "title": title,
                    "url": link,
                    "type": type_lbl
                })
        return results

    # --- 3. VIDEO RESOLVER ---
    def resolve_video_links(self, url):
        soup = self.get_soup(url)
        if not soup: return

        print(f"\n[+] Extracting Players...")
        links = []
        
        # Find standard tabs
        for div in soup.find_all("div", id=re.compile(r"^tab\d+")):
            iframe = div.find('iframe')
            if iframe:
                src = iframe.get('src') or iframe.get('data-src')
                if src and "facebook" not in src:
                    links.append({'label': f"Server {div.get('id')}", 'src': src})
        
        # Fallback iframes
        if not links:
            for i, iframe in enumerate(soup.find_all("iframe"), 1):
                src = iframe.get('src') or iframe.get('data-src')
                if src and "facebook" not in src:
                    links.append({'label': f"Source {i}", 'src': src})

        if not links:
            print("[!] No playable links found.")
            return

        print("\n--- SERVERS ---")
        for i, l in enumerate(links, 1):
            print(f"{i}. {l['label']} : {l['src'][:40]}...")
        
        try:
            c = int(input("\nSelect Server (Number): "))
            if 1 <= c <= len(links):
                self.play_video(links[c-1]['src'])
        except: pass

    # --- 4. SERIES HANDLER ---
    def handle_series(self, url):
        soup = self.get_soup(url)
        if not soup: return

        print("\n[+] Fetching Seasons...")
        seasons = soup.select(".tvseason, .se-c")
        if not seasons:
            self.resolve_video_links(url)
            return

        for i, s in enumerate(seasons, 1):
            lbl = s.find("strong") or s.find(class_="title")
            print(f"   {i}. {lbl.get_text(strip=True) if lbl else f'Season {i}'}")

        try:
            s_idx = int(input("\nSelect Season: ")) - 1
            selected = seasons[s_idx]
        except: return

        # Find episodes
        content = selected.find("div", class_="les-content")
        episodes = content.find_all("a") if content else selected.find_all("li")
        
        ep_urls = []
        print(f"\n--- Episodes ---")
        for i, ep in enumerate(episodes, 1):
            print(f"   {i}. {ep.get_text(strip=True)}")
            ep_urls.append(ep.get('href') or ep.find('a')['href'])

        try:
            e_idx = int(input("\nSelect Episode: ")) - 1
            self.resolve_video_links(ep_urls[e_idx])
        except: return

    # --- 5. MENU ---
    def get_menu(self):
        soup = self.get_soup(BASE_URL)
        if not soup: return [], []
        genres, years = [], []
        
        for a in soup.find_all("a", href=True):
            txt = a.get_text(strip=True)
            href = a['href']
            if not txt or len(txt) > 20: continue
            
            if '/genre/' in href and (txt, href) not in genres: genres.append((txt, href))
            if ('/release-year/' in href or '/year/' in href) and txt.isdigit() and (txt, href) not in years:
                years.append((txt, href))
                
        genres.sort(key=lambda x: x[0])
        years.sort(key=lambda x: x[0], reverse=True)
        return genres, years

    def run(self):
        while True:
            print("\n=== PENCURI MOVIE (Portable) ===")
            print("1. Search")
            print("2. Browse Genres")
            print("3. Browse Years")
            print("4. Exit")
            
            c = input("\nAction > ")
            res = []

            if c == '1': res = self.search(input("Search: "))
            elif c == '2':
                g, _ = self.get_menu()
                for i, (n, _) in enumerate(g, 1): print(f"{i}. {n}")
                try: res = self.parse_listing(g[int(input("Genre: "))-1][1])
                except: continue
            elif c == '3':
                _, y = self.get_menu()
                for i, (n, _) in enumerate(y, 1): print(f"{i}. {n}")
                try: res = self.parse_listing(y[int(input("Year: "))-1][1])
                except: continue
            elif c == '4': sys.exit()

            if not res: print("No results."); continue

            print("\n--- RESULTS ---")
            for r in res: print(f"{r['id']}. [{r['type']}] {r['title']}")
            
            try:
                sel = int(input("\nSelect ID: "))
                target = next((x for x in res if x['id'] == sel), None)
                if not target: raise ValueError
                
                if target['type'] == "SERIES": self.handle_series(target['url'])
                else: self.resolve_video_links(target['url'])
            except: print("Invalid.")

if __name__ == "__main__":
    app = PencuriMovieCLI()
    app.run()
from pathlib import Path
from datetime import datetime, timezone
from zoneinfo import ZoneInfo
from urllib.parse import urljoin
import json, re, sys

from playwright.sync_api import sync_playwright
from openpyxl import Workbook, load_workbook
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.utils import get_column_letter

BASE = "https://ratnagiripolice.gov.in"
ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / "data"
DATA_DIR.mkdir(exist_ok=True)
XLSX = DATA_DIR / "police_stations.xlsx"
JSON_FILE = DATA_DIR / "police_stations.json"

STATIONS = [
    (37, "सायबर"), (38, "देवरुख"), (39, "लांजा"), (40, "राजापूर"),
    (41, "नाटे"), (42, "पूर्णगड"), (43, "रत्नागिरी शहर"),
    (44, "रत्नागिरी ग्रामीण"), (45, "जयगड"), (46, "संगमेश्वर"),
    (47, "गुहागर"), (48, "सावर्डे"), (49, "अलोरे - शिरगाव"),
    (50, "चिपळूण"), (51, "खेड"), (52, "दाभोळ"), (53, "दापोली"),
    (54, "मंडणगड"), (55, "बाणकोट")
]

FIELDS = [
    "id", "station_name", "station_image", "officer_name", "designation",
    "officer_image", "email", "mobile", "alternative_phone", "division",
    "city", "district", "map_url", "source_url", "last_checked_ist",
    "record_status", "error"
]
HEADERS = [
    "ID", "पोलीस स्टेशन", "स्टेशन फोटो URL", "पोलीस निरीक्षक / अधिकारी",
    "पद", "अधिकारी फोटो URL", "ई-मेल", "मोबाईल", "पर्यायी फोन",
    "विभाग", "शहर", "जिल्हा", "Google Map URL", "अधिकृत पेज",
    "शेवटची तपासणी (IST)", "स्थिती", "त्रुटी"
]
LABELS = dict(zip(FIELDS, HEADERS))

def clean(value):
    return re.sub(r"\s+", " ", value or "").strip()

def first_text(page, selectors):
    for selector in selectors:
        try:
            loc = page.locator(selector).first
            if loc.count():
                value = clean(loc.inner_text(timeout=1500))
                if value:
                    return value
        except Exception:
            pass
    return ""

def first_attr(page, selectors, attr):
    for selector in selectors:
        try:
            loc = page.locator(selector).first
            if loc.count():
                value = clean(loc.get_attribute(attr, timeout=1500))
                if value:
                    return value
        except Exception:
            pass
    return ""

def old_records():
    records = {}
    if not XLSX.exists():
        return records
    try:
        ws = load_workbook(XLSX, read_only=True, data_only=True).active
        headers = [clean(c.value) for c in ws[1]]
        header_to_field = {v: k for k, v in LABELS.items()}
        for row in ws.iter_rows(min_row=2, values_only=True):
            item = {}
            for h, v in zip(headers, row):
                key = header_to_field.get(h)
                if key:
                    item[key] = "" if v is None else str(v)
            if item.get("id") not in (None, ""):
                records[int(item["id"])] = item
    except Exception as exc:
        print(f"WARNING: existing workbook could not be read: {exc}")
    return records

def scrape_one(page, station_id, expected_name, checked_at):
    url = f"{BASE}/police-station/?id={station_id}"
    result = {field: "" for field in FIELDS}
    result.update(id=station_id, station_name=expected_name, source_url=url,
                  last_checked_ist=checked_at, record_status="Not checked", error="")
    try:
        response = page.goto(url, wait_until="domcontentloaded", timeout=60000)
        if response and response.status >= 400:
            result["record_status"] = "Fetch failed"
            result["error"] = f"HTTP {response.status}"
            return result

        try:
            page.wait_for_load_state("networkidle", timeout=8000)
        except Exception:
            pass
        try:
            page.wait_for_selector(
                ".about-style2__content, .officer-profile-card, .officer-details-table",
                timeout=12000
            )
        except Exception:
            pass

        heading = first_text(page, [
            ".about-style2__content .sec-title h2",
            ".about-style2__content h2"
        ])
        if heading:
            result["station_name"] = clean(heading.replace("\n", " "))

        station_image = first_attr(page, [
            ".about-style2__img .inner img",
            ".about-style2__img img"
        ], "src")
        result["station_image"] = urljoin(BASE, station_image) if station_image else ""
        result["officer_name"] = first_text(page, [
            ".officer-profile-card .team-stye1__single-content h2 a",
            ".officer-profile-card .team-style1__single-content h2 a",
            ".officer-profile-card h2 a",
            ".officer-profile-card h2"
        ])
        result["designation"] = first_text(page, [
            ".officer-profile-card .designation",
            ".officer-details-card .designation"
        ])
        officer_image = first_attr(page, [
            ".officer-profile-card .team-style1__single-img-inner img",
            ".officer-profile-card img"
        ], "src")
        result["officer_image"] = urljoin(BASE, officer_image) if officer_image else ""

        # Contact links can appear in the officer card or the details table.
        mail_href = first_attr(page, [
            ".officer-profile-card a[href^='mailto:']",
            ".officer-details-table a[href^='mailto:']"
        ], "href")
        if mail_href.startswith("mailto:"):
            result["email"] = mail_href[len("mailto:"):]

        phones = []
        for selector in [
            ".officer-profile-card a[href^='tel:']",
            ".officer-details-table a[href^='tel:']"
        ]:
            try:
                values = page.locator(selector).evaluate_all(
                    "(els) => els.map(e => (e.getAttribute('href') || '').replace(/^tel:/i, '').trim())"
                )
                phones.extend(clean(v) for v in values if clean(v))
            except Exception:
                pass
        phones = list(dict.fromkeys(phones))
        if phones:
            result["mobile"] = phones[0]
        if len(phones) > 1:
            result["alternative_phone"] = phones[1]

        # Table label/value pairs are a reliable source for Division, City, District.
        try:
            rows = page.locator(".officer-details-table tr").evaluate_all(
                """els => els.map(tr => ({
                  label: (tr.querySelector('.detail-label')?.innerText || '').trim(),
                  value: (tr.querySelector('.detail-value')?.innerText || '').trim()
                }))"""
            )
            for row in rows:
                label = clean(row.get("label", "")).lower()
                value = clean(row.get("value", ""))
                if not value:
                    continue
                if "division" in label:
                    result["division"] = value
                elif "city" in label:
                    result["city"] = value
                elif "district" in label:
                    result["district"] = value
                elif "email" in label and not result["email"]:
                    result["email"] = value
                elif "mobile" in label and not result["mobile"]:
                    result["mobile"] = value
                elif "alternative" in label and not result["alternative_phone"]:
                    result["alternative_phone"] = value
        except Exception:
            pass

        result["map_url"] = first_attr(page, [".google-map iframe", "iframe[src*='google.com/maps']"], "src")
        body = first_text(page, ["body"])
        if not heading and ("No police station data found" in body or len(body) < 80):
            result["record_status"] = "No station record on page"
            result["error"] = "Official page did not expose a station record"
        else:
            found = sum(bool(result[k]) for k in [
                "officer_name", "designation", "email", "mobile",
                "alternative_phone", "division", "city", "district",
                "officer_image", "map_url"
            ])
            result["record_status"] = f"Extracted {found} fields" if found else "Page opened; target fields not found"
        return result
    except Exception as exc:
        result["record_status"] = "Fetch failed"
        result["error"] = str(exc)[:400]
        return result

def merge_preserving_existing(new, old):
    merged = dict(old or {})
    for key in FIELDS:
        if new.get(key) not in (None, ""):
            merged[key] = new[key]
        elif key not in merged:
            merged[key] = ""
    # Keep the newly checked timestamp/status/error, but do not blank out old contact data.
    for key in ("last_checked_ist", "record_status", "error", "source_url", "id"):
        merged[key] = new.get(key, merged.get(key, ""))
    if not new.get("error"):
        merged["error"] = ""
    return merged

def write_outputs(records):
    ordered = [records[sid] for sid, _ in STATIONS]
    JSON_FILE.write_text(
        json.dumps(ordered, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )
    wb = Workbook()
    ws = wb.active
    ws.title = "Police Stations"
    ws.append(HEADERS)
    for record in ordered:
        ws.append([record.get(field, "") for field in FIELDS])
    for cell in ws[1]:
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = PatternFill("solid", fgColor="17365D")
        cell.alignment = Alignment(wrap_text=True, vertical="center")
    ws.freeze_panes = "A2"
    ws.auto_filter.ref = ws.dimensions
    widths = [8, 22, 48, 24, 24, 48, 38, 18, 18, 18, 18, 18, 55, 55, 24, 30, 45]
    for index, width in enumerate(widths, 1):
        ws.column_dimensions[get_column_letter(index)].width = width
    for row in ws.iter_rows(min_row=2):
        for cell in row:
            cell.alignment = Alignment(vertical="top", wrap_text=True)

    readme = wb.create_sheet("Read Me")
    readme.append(["माहिती स्रोत", BASE])
    readme.append(["अपडेट वेळ", "दर रविवारी सकाळी ६:०० IST"])
    readme.append(["महत्त्वाची सूचना", "ही फाईल अधिकृत वेबसाइटवरील निवडक HTML घटकांमधून तयार होते."])
    readme.append(["रिकामी माहिती", "माहिती सापडली नाही; त्यामुळे ती अस्तित्वात नाही असा निष्कर्ष काढू नये."])
    readme.append(["जुना डेटा", "नवीन पेजवर फील्ड रिकामे असल्यास आधीची उपलब्ध माहिती जपली जाते."])
    readme.append(["पडताळणी", "स्वयंचलित extraction म्हणजे अधिकृत पुष्टी नव्हे. वापरापूर्वी अधिकृत पेज तपासा."])
    wb.save(XLSX)

def main():
    checked_at = datetime.now(ZoneInfo("Asia/Kolkata")).strftime("%Y-%m-%d %H:%M:%S")
    previous = old_records()
    fresh = {}
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(locale="mr-IN", viewport={"width": 1440, "height": 1000})
        for station_id, name in STATIONS:
            print(f"Checking {station_id}: {name}")
            fresh[station_id] = scrape_one(page, station_id, name, checked_at)
        browser.close()

    successes = sum(1 for item in fresh.values() if item["record_status"].startswith("Extracted"))
    opened = sum(1 for item in fresh.values() if item["record_status"] != "Fetch failed")
    if opened == 0:
        print("ERROR: all station pages failed. Existing data files will not be overwritten.")
        sys.exit(2)

    merged = {}
    for station_id, _ in STATIONS:
        merged[station_id] = merge_preserving_existing(fresh[station_id], previous.get(station_id))
    write_outputs(merged)
    print(f"Finished. Pages opened: {opened}/{len(STATIONS)}; records with extracted fields: {successes}.")
    print(f"Updated: {XLSX} and {JSON_FILE}")

if __name__ == "__main__":
    main()

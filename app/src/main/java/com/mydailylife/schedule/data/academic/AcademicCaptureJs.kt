package com.mydailylife.schedule.data.academic

/**
 * Injected into the academic WebView.
 * - Hooks XHR/fetch so timetable API bodies are buffered
 * - On capture, returns buffered network payloads + a DOM table scrape
 *   (including weekday×period grid layouts used by BNU `xskcb` JSP pages)
 */
object AcademicCaptureJs {
    const val BRIDGE_NAME = "MdlAcademicBridge"

    /** Run after each page load (idempotent). */
    val INSTALL_HOOKS: String = """
        (function(){
          if (window.__MDL_ACADEMIC_HOOKED) return true;
          window.__MDL_ACADEMIC_HOOKED = true;
          window.__MDL_CAPTURED = [];
          function interesting(url, body) {
            if (!body || body.length < 40) return false;
            var u = (url || '').toLowerCase();
            // Ignore portal i18n / noise (often contains 星期一 in translations).
            if (/i18n|message\.json|zh_message|en_message|language\/|themeservlet|popmenuservlet|getbreadcrumbs/.test(u)) return false;
            if (/xskcb|wsxk\.xskcb|student\/wsxk/.test(u)) return true;
            if (/my-table-detail|table-detail|classTimetableVOList/.test(u)) return true;
            // Strong BNU timetable HTML markers only
            if (/id=["']mytable["']/.test(body) && /星期一/.test(body)) return true;
            if (/padding-bottom\s*:\s*5px/.test(body) && /\d+\s*\[\s*\d+/.test(body)) return true;
            if (/学生选课课程表/.test(body) && /div_nokb/.test(body)) return true;
            return /"classTimetableVOList"|"courseName"|"kcmc"/.test(body) && /"weekDay"|"weekday"|"xqj"/.test(body);
          }
          function keep(url, body) {
            try {
              if (!interesting(url, body)) return;
              window.__MDL_CAPTURED.push({ url: String(url||''), body: String(body), t: Date.now() });
              if (window.__MDL_CAPTURED.length > 30) window.__MDL_CAPTURED.shift();
              if (window.MdlAcademicBridge && window.MdlAcademicBridge.onNetworkPayload) {
                window.MdlAcademicBridge.onNetworkPayload(String(url||''), String(body));
              }
            } catch (e) {}
          }
          var ox = XMLHttpRequest.prototype.open;
          var xs = XMLHttpRequest.prototype.send;
          XMLHttpRequest.prototype.open = function(method, url) {
            this.__mdl_url = url;
            return ox.apply(this, arguments);
          };
          XMLHttpRequest.prototype.send = function() {
            var self = this;
            this.addEventListener('load', function() {
              try { keep(self.__mdl_url, self.responseText); } catch (e) {}
            });
            return xs.apply(this, arguments);
          };
          if (window.fetch) {
            var ofetch = window.fetch;
            window.fetch = function() {
              var args = arguments;
              var req = args[0];
              var url = typeof req === 'string' ? req : (req && req.url);
              return ofetch.apply(this, args).then(function(res) {
                try {
                  var clone = res.clone();
                  clone.text().then(function(t) { keep(url, t); }).catch(function(){});
                } catch (e) {}
                return res;
              });
            };
          }
          return true;
        })();
    """.trimIndent()

    /**
     * Push `#mytable` (or BNU-like grid) HTML to the native bridge.
     * Prefer this over returning large HTML via evaluateJavascript (size limits).
     * Call on every page finished and again right before capture.
     * Returns number of characters pushed (0 if none).
     */
    val PUSH_TIMETABLE_HTML: String = """
        (function(){
          function findTableHtml(doc) {
            if (!doc) return '';
            try {
              var t = doc.getElementById('mytable');
              if (t && t.outerHTML) return t.outerHTML;
            } catch (e) {}
            try {
              var tables = doc.querySelectorAll('table');
              for (var i = 0; i < tables.length; i++) {
                var html = tables[i].outerHTML || '';
                if (html.indexOf('星期一') >= 0 &&
                    (html.indexOf('padding-bottom') >= 0 || html.indexOf('div_nokb') >= 0 || /\d+\s*\[\s*\d+/.test(html))) {
                  if (html.indexOf('mytable') < 0) {
                    return '<table id="mytable">' + (tables[i].innerHTML || '') + '</table>';
                  }
                  return html;
                }
              }
            } catch (e) {}
            return '';
          }
          function pushDoc(doc, href) {
            try {
              var html = findTableHtml(doc);
              if (!html || html.length < 80) return 0;
              if (window.MdlAcademicBridge && window.MdlAcademicBridge.onNetworkPayload) {
                window.MdlAcademicBridge.onNetworkPayload(String(href || ''), html);
              }
              return html.length;
            } catch (e) { return 0; }
          }
          // BNU 教务是 frameset：课表在子 frame，不在顶层 document。
          function walk(win, depth) {
            var n = 0;
            if (!win || depth > 8) return 0;
            try {
              var href = '';
              try { href = win.location && win.location.href; } catch (e) { href = ''; }
              n += pushDoc(win.document, href);
            } catch (e) {}
            var len = 0;
            try { len = win.frames ? win.frames.length : 0; } catch (e) { len = 0; }
            for (var i = 0; i < len; i++) {
              try { n += walk(win.frames[i], depth + 1); } catch (e) {}
            }
            return n;
          }
          return walk(window, 0);
        })();
    """.trimIndent()

    /**
     * User-triggered capture. Returns a JSON string (may be quoted by evaluateJavascript).
     * Large BNU HTML is pushed via [PUSH_TIMETABLE_HTML] / bridge, not embedded here.
     */
    val CAPTURE_NOW: String = """
        (function(){
          function textOf(el){ return (el && (el.innerText || el.textContent) || '').replace(/\s+/g,' ').trim(); }
          function cellLines(el){
            var raw = (el && (el.innerText || el.textContent) || '').replace(/\r/g,'');
            return raw.split(/\n+/).map(function(s){ return s.replace(/\s+/g,' ').trim(); }).filter(Boolean);
          }
          function weekdayFromHeader(h){
            if (!h) return 0;
            var t = String(h).replace(/\s+/g,'');
            if (/周一|星期一|^一$|^1$/.test(t)) return 1;
            if (/周二|星期二|^二$|^2$/.test(t)) return 2;
            if (/周三|星期三|^三$|^3$/.test(t)) return 3;
            if (/周四|星期四|^四$|^4$/.test(t)) return 4;
            if (/周五|星期五|^五$|^5$/.test(t)) return 5;
            if (/周六|星期六|^六$|^6$/.test(t)) return 6;
            if (/周日|星期日|星期天|^日$|^天$|^7$/.test(t)) return 7;
            return 0;
          }
          function slotFromLabel(label, rowIndex){
            if (!label) return null;
            var t = String(label).replace(/\s+/g,'');
            var m = t.match(/(\d+)\s*[-～~—到至]\s*(\d+)/);
            if (m) return { start: +m[1], end: +m[2] };
            m = t.match(/第?\s*(\d+)\s*节?/);
            if (m) return { start: +m[1], end: +m[1] };
            if (/^\d{1,2}$/.test(t)) return { start: +t, end: +t };
            return rowIndex > 0 ? { start: rowIndex, end: rowIndex } : null;
          }
          function parseCellCourse(lines, weekday, slot){
            if (!lines || !lines.length) return null;
            var joined = lines.join(' ');
            if (joined.length < 2) return null;
            if (/^[\-—–．.]+$/.test(joined)) return null;
            var title = lines[0];
            var teacher = '', location = '', weeks = '';
            for (var i = 1; i < lines.length; i++) {
              var line = lines[i];
              if (/\d+\s*[-～~—到至,，]?\s*\d*\s*周/.test(line) || /单周|双周/.test(line) || /\[\d+/.test(line)) {
                weeks = weeks || line;
              } else if (/楼|室|区|馆|场|厅|教室|教学|实验|操场|A\d|B\d|C\d|D\d|E\d/.test(line) || /\d{2,}/.test(line) && /[A-Za-z\u4e00-\u9fa5]/.test(line)) {
                if (!location) location = line; else if (!teacher) teacher = line;
              } else if (!teacher) {
                teacher = line;
              } else if (!location) {
                location = line;
              }
            }
            if (!weeks) {
              var wm = joined.match(/(\d+\s*[-～~—到至,，\d]*\s*周|单周|双周|\d[\d,，\-]*\[\d)/);
              if (wm) weeks = wm[1];
            }
            return {
              '课程名': title,
              '教师': teacher,
              '地点': location,
              '星期': String(weekday),
              '开始节次': String(slot.start),
              '结束节次': String(slot.end),
              '节次': slot.start + '-' + slot.end,
              '周次': weeks,
              'rawText': joined
            };
          }
          function scrapeGrid(table){
            var rows = table.querySelectorAll('tr');
            if (rows.length < 2) return [];
            var headerCells = rows[0].querySelectorAll('th,td');
            var headers = [];
            for (var h = 0; h < headerCells.length; h++) headers.push(textOf(headerCells[h]));
            var weekCols = [];
            for (var c = 0; c < headers.length; c++) {
              var wd = weekdayFromHeader(headers[c]);
              if (wd) weekCols.push({ col: c, weekday: wd });
            }
            if (weekCols.length < 3) return [];
            var out = [];
            for (var r = 1; r < rows.length; r++) {
              var cells = rows[r].querySelectorAll('td,th');
              if (!cells.length) continue;
              var slotLabel = textOf(cells[0]);
              var slot = slotFromLabel(slotLabel, r);
              if (!slot) continue;
              for (var wi = 0; wi < weekCols.length; wi++) {
                var meta = weekCols[wi];
                if (meta.col >= cells.length) continue;
                var lines = cellLines(cells[meta.col]);
                if (lines.length >= 6) {
                  var chunks = [];
                  var cur = [];
                  for (var li = 0; li < lines.length; li++) {
                    var L = lines[li];
                    if (cur.length >= 3 && !/\d+\s*周|单周|双周|楼|室|\[/.test(L) && cur.length) {
                      chunks.push(cur); cur = [L];
                    } else {
                      cur.push(L);
                    }
                  }
                  if (cur.length) chunks.push(cur);
                  for (var ci = 0; ci < chunks.length; ci++) {
                    var course = parseCellCourse(chunks[ci], meta.weekday, slot);
                    if (course) out.push(course);
                  }
                } else {
                  var one = parseCellCourse(lines, meta.weekday, slot);
                  if (one) out.push(one);
                }
              }
            }
            return out;
          }
          function scrapeListTable(table){
            var courses = [];
            var rows = table.querySelectorAll('tr');
            if (rows.length < 2) return courses;
            var headerCells = rows[0].querySelectorAll('th,td');
            var headers = [];
            for (var h = 0; h < headerCells.length; h++) headers.push(textOf(headerCells[h]));
            var headerJoined = headers.join('|');
            var looksCourse = /课程|课名|course|教师|老师|教室|地点|星期|周|节次/.test(headerJoined);
            if (!looksCourse && rows.length < 3) return courses;
            for (var r = 1; r < rows.length; r++) {
              var cells = rows[r].querySelectorAll('td,th');
              if (!cells.length) continue;
              var vals = [];
              for (var c = 0; c < cells.length; c++) vals.push(textOf(cells[c]));
              if (!vals.join('')) continue;
              var obj = { raw: vals };
              for (var i = 0; i < headers.length && i < vals.length; i++) {
                obj[headers[i] || ('c'+i)] = vals[i];
              }
              if (!obj['课程名'] && vals[0]) obj['课程名'] = vals[0];
              courses.push(obj);
            }
            return courses;
          }
          function scrapeDom() {
            var courses = [];
            var tables = document.querySelectorAll('table');
            for (var ti = 0; ti < tables.length; ti++) {
              var grid = scrapeGrid(tables[ti]);
              if (grid.length) {
                for (var g = 0; g < grid.length; g++) courses.push(grid[g]);
                continue;
              }
              var listed = scrapeListTable(tables[ti]);
              for (var l = 0; l < listed.length; l++) courses.push(listed[l]);
            }
            if (courses.length === 0) {
              var nodes = document.querySelectorAll('[class*="course"],[class*="Course"],[class*="timetable"] [class*="item"],[class*="kebiao"]');
              for (var n = 0; n < nodes.length && n < 200; n++) {
                var t = textOf(nodes[n]);
                if (t.length >= 2 && t.length < 80) courses.push({ 课程名: t, rawText: t });
              }
            }
            return courses;
          }
          // Prefer bridge push for large HTML; do not embed pageHtml in the return value.
          try {
            var t = document.getElementById('mytable');
            if (t && t.outerHTML && window.MdlAcademicBridge && window.MdlAcademicBridge.onNetworkPayload) {
              window.MdlAcademicBridge.onNetworkPayload(String(location.href||''), t.outerHTML);
            }
          } catch (e) {}
          var payload = {
            pageUrl: location.href,
            pageTitle: document.title || '',
            pageHtml: '',
            network: window.__MDL_CAPTURED || [],
            domCourses: scrapeDom()
          };
          return JSON.stringify(payload);
        })();
    """.trimIndent()
}

package com.mydailylife.schedule.data.academic

/**
 * Injected into the academic WebView.
 * - Hooks XHR/fetch so timetable API bodies are buffered
 * - On capture, returns buffered network payloads + a DOM table scrape
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
            if (/my-table-detail|table-detail|timetable|schedule|course|kebiao|classinfo|teaching|student\/course|选课|课表|xk\/|jwgl/.test(u)) return true;
            return /"classTimetableVOList"|"courseName"|"kcmc"|"jxbmc"|"classroom"|"placeName"|"roomName"|"teachingWeek"|"periodFormat"|"classTime"|"skjc"|"weekday"|"weekDay"|"xqj"|"weeks"|"teacherName"|"instructorName"|"jsxm"/.test(body);
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
     * User-triggered capture. Returns a JSON string (may be quoted by evaluateJavascript).
     */
    val CAPTURE_NOW: String = """
        (function(){
          function textOf(el){ return (el && (el.innerText || el.textContent) || '').replace(/\s+/g,' ').trim(); }
          function scrapeDom() {
            var courses = [];
            var tables = document.querySelectorAll('table');
            for (var ti = 0; ti < tables.length; ti++) {
              var table = tables[ti];
              var rows = table.querySelectorAll('tr');
              if (rows.length < 2) continue;
              var headerCells = rows[0].querySelectorAll('th,td');
              var headers = [];
              for (var h = 0; h < headerCells.length; h++) headers.push(textOf(headerCells[h]));
              var headerJoined = headers.join('|');
              var looksCourse = /课程|课名|course|教师|老师|教室|地点|星期|周|节次/.test(headerJoined);
              if (!looksCourse && rows.length < 3) continue;
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
            }
            // Grid / card fallback: elements that look like course blocks
            if (courses.length === 0) {
              var nodes = document.querySelectorAll('[class*="course"],[class*="Course"],[class*="timetable"] [class*="item"],[class*="kebiao"]');
              for (var n = 0; n < nodes.length && n < 200; n++) {
                var t = textOf(nodes[n]);
                if (t.length >= 2 && t.length < 80) courses.push({ 课程名: t, rawText: t });
              }
            }
            return courses;
          }
          var payload = {
            pageUrl: location.href,
            pageTitle: document.title || '',
            network: window.__MDL_CAPTURED || [],
            domCourses: scrapeDom()
          };
          return JSON.stringify(payload);
        })();
    """.trimIndent()
}

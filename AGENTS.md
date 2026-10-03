# HIẾN PHÁP DÀNH CHO AI AGENT (AGENTS.MD)

Tài liệu này là Hiến pháp tối cao dành cho tất cả AI Agent làm việc trong dự án theo chuẩn **Development Workspace Standard V2.1.2**.

---

## 1. THỨ TỰ ĐỌC BẮT BUỘC TRƯỚC KHI THỰC HIỆN BẤT KỲ TASK NÀO

1. Đọc [README.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/README.md)
2. Đọc [AGENTS.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/AGENTS.md)
3. Đọc [Docs/rules.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/Docs/rules.md)
4. Đọc [PROJECT_ERROR.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/PROJECT_ERROR.md) để tránh lặp lại các lỗi đã biết
5. Đọc [ACQUIREMENTS.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/ACQUIREMENTS.md) để nạp các giải pháp và tiêu chuẩn đã kiểm chứng
6. Kiểm tra [.ai/locks.json](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/.ai/locks.json)
7. Kiểm tra `git status` và branch đang làm việc
8. Kiểm tra acceptance criteria của task từ [UPDATETODOS.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/UPDATETODOS.md)
9. Trước khi kết thúc task, đánh giá và cập nhật [PROJECT_ERROR.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/PROJECT_ERROR.md) và [ACQUIREMENTS.md](file:///d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/ACQUIREMENTS.md) nếu có phát hiện lỗi hoặc bài học mới.

---

## 2. 24 LUẬT CỨNG (HARD RULES)

1. Không sửa code/config/data nếu không có TASK ID hợp lệ.
2. Không sửa file ngoài phạm vi task được giao.
3. Không sửa file đang bị khóa (locked).
4. Không commit trực tiếp lên nhánh `main` khi đang phát triển tính năng.
5. Không sửa test chỉ để test pass ("làm xanh").
6. Không xóa test đang fail.
7. Không bỏ qua (skip/disable) test mà không có phê duyệt.
8. Không tắt linter, typecheck hoặc security check.
9. Không cài dependency mới nếu không có lý do chính đáng.
10. Không tự ý thay đổi kiến trúc nếu không có ADR.
11. Không thay đổi schema database nếu không có migration.
12. **TUYỆT ĐỐI KHÔNG để lộ secrets, passwords, keystore (*.jks) hoặc credentials.**
13. Không chạy lệnh Git mang tính phá hủy (destructive).
14. Không dùng `git push --force`.
15. Luôn chạy kiểm tra (verify/build/test) trước khi commit.
16. Luôn cập nhật trạng thái task (`.ai/state.json` và `UPDATETODOS.md`).
17. Luôn cập nhật changelog cho các thay đổi quan trọng.
18. Luôn tạo handoff note trước khi kết thúc phiên chưa hoàn tất.
19. Dừng vòng lặp tự sửa lỗi nếu vượt quá số lần cấu hình (Loop Guard).
20. Nếu không chắc chắn, hãy kiểm tra code/docs thực tế; tuyệt đối không tự "đoán" trạng thái.
21. Luôn đọc `PROJECT_ERROR.md` và `ACQUIREMENTS.md` trước khi sửa đổi.
22. Không đánh dấu lỗi RESOLVED nếu chưa có phương án kiểm chứng lặp lại và biện pháp ngăn ngừa.
23. Luôn cập nhật tri thức bền vững trong cùng commit/PR khi phát hiện bài học mới.
24. Không lưu thông tin nhạy cảm vào file bộ nhớ của Agent.

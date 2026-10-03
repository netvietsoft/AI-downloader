const fs = require('fs');

const layoutPath = 'D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/res/layout/fragment_home.xml';
let layout = fs.readFileSync(layoutPath, 'utf8');

// Replace the EditText, ImageView, TextView in search bar
const oldBlockRegex = /<EditText[\s\S]*?android:id=\"@\+id\/btn_go_url\"[\s\S]*?\/>/;
const newBlock = `<EditText
                    android:id="@+id/et_url_input"
                    android:layout_width="0dp"
                    android:layout_height="match_parent"
                    android:layout_marginStart="10dp"
                    android:layout_weight="1"
                    android:background="@null"
                    android:hint="Tìm kiếm Google hoặc nhập URL..."
                    android:imeOptions="actionSearch"
                    android:inputType="textUri"
                    android:maxLines="1"
                    android:textColor="@color/text_white"
                    android:textColorHint="@color/text_muted"
                    android:textSize="13sp" />

                <!-- Nút xóa nhanh URL (dấu ✕) -->
                <TextView
                    android:id="@+id/btn_clear_url"
                    android:layout_width="30dp"
                    android:layout_height="30dp"
                    android:layout_marginEnd="6dp"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:gravity="center"
                    android:text="✕"
                    android:textColor="@color/text_muted"
                    android:textSize="15sp"
                    android:textStyle="bold"
                    android:visibility="gone" />

                <TextView
                    android:id="@+id/btn_go_url"
                    android:layout_width="wrap_content"
                    android:layout_height="32dp"
                    android:background="@drawable/bg_pill_btn"
                    android:gravity="center"
                    android:paddingStart="14dp"
                    android:paddingEnd="14dp"
                    android:text="SEARCH"
                    android:textColor="#FFFFFF"
                    android:textSize="12sp"
                    android:textStyle="bold" />`;

layout = layout.replace(oldBlockRegex, newBlock);
fs.writeFileSync(layoutPath, layout, 'utf8');
console.log('fragment_home.xml updated successfully');

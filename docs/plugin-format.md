# 插件格式（format 2）

插件包是扩展名为 `.melodiaplugin` 的 zip 文件，在「设置 → 扩展 → 插件」中导入。导入时会先显示插件需要的权限，确认后才安装并启用。

## 包结构

| 文件 | 说明 | 上限 |
|---|---|---|
| `plugin.json` | 清单，必需 | 256KB |
| `main.js` | 脚本入口；声明了页面、音源或事件时必需 | 2MB |
| `ui/*.json` | 页面与布局描述，只允许一层 | 单个 256KB |
| `assets/*.png/jpg/jpeg/webp` | 图片，只允许一层 | 单个 2MB |

整个包最多 64 个文件，解压后不超过 10MB。白名单之外的文件会被忽略，并在导入时提示。

## 清单

```json
{
  "format": 1,
  "id": "com.example.lastfm",
  "name": "Last.fm 记录",
  "version": "1.0.0",
  "author": "someone",
  "description": "把听过的歌记录到 Last.fm",
  "minAppVersion": "1.6.0",
  "permissions": ["network", "playback.read", "storage"],
  "network": { "hosts": ["ws.audioscrobbler.com"] },
  "contributes": {
    "playerLayouts": { "android.miniPlayer": "ui/bar.json" },
    "pages": [{ "id": "stats", "title": "听歌统计", "icon": "chart", "ui": "ui/stats.json", "placement": "sidebar" }],
    "audioSources": [{ "id": "main", "name": "示例音源" }],
    "events": ["songChanged", "playStateChanged"]
  }
}
```

| 字段 | 说明 |
|---|---|
| `format` | 必需，`1` 或 `2`；高于应用支持的版本时拒绝导入 |
| `id` | 必需；反向域名形式，只能用小写字母、数字和 `_`，如 `com.example.demo`。同 id 再次导入即升级 |
| `name` / `version` | 必需 |
| `author` / `description` | 可选 |
| `minAppVersion` | 可选，低于这个版本的应用不能安装；开发构建不校验 |
| `permissions` | 见下文；未知权限会被忽略并提示 |
| `network.hosts` | 申请 `network` 时必需；`*.example.com` 只匹配子域名，不匹配 `example.com` 本身 |
| `contributes` | 至少提供一项能力，否则插件无效 |
| `platforms` | format 2；可选，目前只支持 `android`，不写表示所有平台。不包含当前平台时拒绝导入，未知平台会被忽略并提示 |
| `apiVersion` | format 2；写成 `1.0`，可选。主版本必须与应用一致，次版本不能高于应用，否则拒绝导入。当前应用提供 `1.9` |

format 1 的插件继续有效，其中的 `kind`、`platforms`、`apiVersion` 会被忽略。

### contributes

- `playerLayouts`：key 为区域名（`android.fullPlayer`、`android.fullPlayerWide`、`android.miniPlayer`），值为 `ui/` 下的文件路径，或直接内联的描述对象。
- `pages`：每项需要 `id`（小写字母、数字、`-`、`_`，不超过 32 个字符）、`title` 与 `ui`。`placement` 可选，取值为 `sidebar`（默认）、`nowPlayingCard` 或 `settings`（API 1.4 起）。
- `audioSources`：每项需要 `id`，`name` 可选。
- `events`：`songChanged`、`playStateChanged`、`progress`。
- `commands`（format 2）：每项需要 `id` 与 `title`，脚本用 `melodia.register.command` 实现。
- `menus`（format 2）：每项需要 `command` 与 `location`。`location` 取 `song` 或 `toolbar`。只显示脚本已注册的命令。`song` 出现在 Android 歌曲「更多」弹层与全屏播放页「更多」弹层（播客节目除外）；`toolbar` 是「设置 → 扩展 → 插件」里的「插件命令」。
- `settings`（format 2）：插件设置项，见「设置」一节。
- `contentSources`（format 2）：第三方内容，见「内容源」一节。每项需要 `id`，`name` 可选，`search` 为 `true` 时参与搜索。
- `layoutActions`（format 2）：播放器布局的手势里用到 `script:` 动作时写 `true`，插件需要带 `main.js`，见「手势」。

单项写错时只忽略这一项，并在导入时提示。

## 权限

| 权限 | 允许的操作 | 对应能力 |
|---|---|---|
| `network` | 访问 `network.hosts` 列出的域名 | |
| `playback.read` | 读取当前歌曲、播放状态与进度 | `events` 必需 |
| `playback.control` | 播放、暂停、切歌、加入队列 | |
| `storage` | 插件私有的键值存储，上限 1MB | |
| `audioSource` | 作为音源提供播放链接 | `audioSources` 必需 |
| `ui.notify` | 显示应用内提示，提示前会带上插件名称 | |
| `ui.openUrl` | 用系统浏览器打开网页，每次打开前询问用户 | |
| `library.read` | 读取当前用户的歌单与红心列表 | |
| `contentSource` | 提供歌曲、歌单与搜索结果 | `contentSources` 必需 |
| `netease.read` | 以当前用户身份调用网易只读方法 | |
| `netease.write` | 以当前用户身份调用网易写方法 | |

提供了某项能力却没有申请对应权限时，整个插件无效。插件升级后如果申请了新的权限，导入预览会标出新增的权限。

## 脚本

`main.js` 在隔离的 JS 环境中运行：Android 上是无界面的 WebView。脚本不能访问文件系统、Cookie、账号信息与宿主类，只能通过全局对象 `melodia` 调用宿主能力。所有调用都返回 Promise。

```js
await melodia.http.request({ url, method, headers, body })   // 返回 { status, headers, body }
await melodia.storage.get(key)
await melodia.storage.set(key, value)                        // value 为 null 等同于删除
await melodia.storage.remove(key)
await melodia.player.current()                               // { id, songId, title, artist, cover } 或 null
await melodia.player.state()                                 // { playing, positionMs, durationMs }
await melodia.player.play() / pause() / next() / previous()
await melodia.player.enqueue({ songId, title, artist, cover })
await melodia.ui.setState(pageId, patch)                     // 浅合并到页面数据
await melodia.ui.toast(text)                                 // 不超过 120 个字符
await melodia.ui.openUrl(url)                                // 只接受 https，需要 API 1.4
await melodia.settings.get(key)
await melodia.settings.all()
await melodia.netease.call(method, params)
await melodia.library.likedSongIds()
await melodia.library.playlists()
await melodia.content.open(source, id, title)
melodia.on('songChanged', handler)
melodia.register.audioSource('main', { resolve, search })
melodia.register.action('refresh', handler)
melodia.register.command('share', handler)
melodia.register.contentSource('main', { search, playlist, resolve })
```

- 网络请求不会自动跟随到白名单外的域名；每次重定向都会重新核对域名。响应体上限 4MB。
- 宿主调用失败时 Promise 会被拒绝，错误信息说明原因（如缺少权限、不允许访问的域名）。
- 脚本加载失败（语法错误、加载时抛错或超过 8 秒）时，插件会被自动停用。
- 宿主调用插件注册的能力时，如果插件出错或超时，计为一次失败；连续失败 5 次后插件会被自动停用。重新启用后计数清零。
- 订阅了播放事件的插件，脚本连续 5 次超过 2 秒没有响应时也会被自动停用。
- 同一时间未完成的宿主调用不能超过 512 个，超出时插件会被自动停用。
- 脚本只能经 `melodia.http.request` 联网。Android 沙盒里 `fetch`、`XMLHttpRequest`、`WebSocket`、外部图片和页面跳转都不可用；`localStorage` 只有插件自己能读写，删除插件时清除，需要长期保存的数据请用 `melodia.storage`。
- 沙盒插件给出的封面地址同样要在 `network.hosts` 内，否则按没有封面处理。

## 音源

```js
melodia.register.audioSource('main', {
  resolve: async function(song) {
    // song: { songId, title, artists, album, durationMs, origin, quality }
    return 'https://...';            // 或 { url: 'https://...' }
  }
});
```

- 只在官方链接不可用或只有试听、且开启了换源时调用，与 LX 插件一起参与社区源轮询；用户打开「插件优先于官方音源」后，每首歌都会先调用。
- `origin` 为 `songId` 所属平台（`netease`、`qq`、`kugou`、`kuwo`、`migu`）；`quality` 为当前音质档位（`standard`、`exhigh`、`lossless`、`hires`）。
- 只接受 http(s) 链接；返回空、试听链接或超过 8 秒没有返回时视为没有结果，继续尝试下一个音源。
- 多个插件音源按插件名称顺序尝试；`register` 的名称必须在清单 `audioSources` 里声明过。

## 网易接口

插件拿不到 Cookie 和登录凭证，只能调用下表方法。未登录时调用会被拒绝，响应只包含表里列出的数据。

```js
const page = await melodia.netease.call('search', { keyword: '晴天', type: 'song' });
await melodia.netease.call('song.like', { id: page.items[0].songId, like: true });
```

| 方法 | 权限 | 参数 | 返回 |
|---|---|---|---|
| `song.detail` | `netease.read` | `ids`（最多 200 个） | 歌曲数组 |
| `search` | `netease.read` | `keyword`，`type`（`song`、`album`、`artist`、`playlist`，默认 `song`），`offset`，`limit`（最多 100） | `{ total, hasMore, items }` |
| `playlist.detail` / `album.detail` | `netease.read` | `id` | 歌单对象 |
| `artist.detail` / `artist.topSongs` | `netease.read` | `id` | 歌手对象 / 歌曲数组 |
| `user.playlists` | `netease.read` | | 歌单数组 |
| `user.likedSongs` | `netease.read` | | 歌曲 id 数组 |
| `song.like` | `netease.write` | `id`，`like` | |
| `playlist.create` | `netease.write` | `name`，`privacy`（`0` 公开，`10` 隐私） | 歌单对象 |
| `playlist.addTracks` / `playlist.removeTracks` | `netease.write` | `id`，`trackIds`（最多 200 个） | |
| `playlist.subscribe` | `netease.write` | `id`，`subscribe` | |
| `album.subscribe` | `netease.write` | `id`，`subscribe` | |
| `artist.follow` | `netease.write` | `id`，`follow` | |
| `playlist.delete` | `netease.write` | `id` | |

写方法的限制：

- 导入时确认权限后，插件第一次写入还会再弹一次确认，同意后记住，重新安装插件时失效。
- `playlist.delete` 每次都要用户确认，不能删除「我喜欢的音乐」。
- `playlist.addTracks`、`removeTracks`、`delete` 只能操作用户自己创建的歌单。
- 每个插件每分钟最多写入 30 次、读取 120 次，超出时调用被拒绝。
- 用户拒绝写入后 30 秒内，新的写入请求直接失败，不再弹窗。
- 确认弹窗 60 秒未响应按拒绝处理。

歌曲对象为 `{ songId, title, artist, artists, album, albumId, cover, durationMs }`。

## 内容源

内容源让第三方平台的内容进入应用：搜索结果显示在搜索页的「自定义插件」标签，歌单在原生详情页打开，歌曲可以播放和加入队列。

```js
melodia.register.contentSource('main', {
  search: async function(query) {            // { keyword, offset, limit }，清单里 search 为 true 才调用
    return [{ id: '1', title: '歌名', artist: '歌手', album: '专辑', cover: 'https://...', durationMs: 200000 }];
  },
  playlist: async function(args) {           // { id }
    return { name: '歌单', cover: 'https://...', description: '简介', tracks: [/* 同上 */] };
  },
  resolve: async function(track) {           // { id, quality }
    return 'https://...';                   // 或 { url: 'https://...' }
  }
});

// 打开原生歌单详情页，通常放在命令或页面动作里
await melodia.content.open('main', 'playlist-1', '我的歌单');
```

- 歌曲的 `id` 与 `title` 必需，缺少时跳过这一项；每次最多 1000 首。
- `resolve` 只接受 http(s) 链接，返回空或试听链接时无法播放。
- 每次调用的上限是 8 秒，超时和出错都计入连续出错。
- 详情页没有评论、收藏和分享入口；重启应用后之前打开的插件歌单需要重新进入。
- 插件停用后，已在队列里的歌曲无法继续解析。

## 打开网页

```js
await melodia.ui.openUrl('https://www.last.fm/api/auth/?api_key=...&token=...');
```

- 需要 `ui.openUrl` 权限与 API 1.4，只接受 `https`，网址不超过 4096 个字符；不要求在 `network.hosts` 内。
- 每次调用都会弹窗，显示插件名和网址的域名（不显示路径与参数），用户同意后才打开；拒绝或 60 秒未响应时 Promise 被拒绝，之后 30 秒内的调用直接失败。
- 用户拒绝后 30 秒内，同一插件的新请求直接失败，不再弹窗；同一插件已有请求在等待确认时，新请求也直接失败。
- 打开成功只代表交给了系统浏览器，插件拿不到网页里的任何内容；需要结果时自己轮询接口。

## 命令

```json
"contributes": {
  "commands": [{ "id": "scrobble", "title": "记录到 Last.fm" }],
  "menus": [{ "command": "scrobble", "location": "song" }]
}
```

```js
melodia.register.command('scrobble', async function(song) {
  // song 只在 location 为 song 时有值：{ songId, title, artist, album, cover }
});
```

外部平台的歌曲没有 `songId`。命令 5 秒内没有返回或抛错时，应用会提示执行失败，并计入连续出错。

## 设置

设置项写在清单里，应用生成设置页，入口在插件列表的设置按钮。

```json
"settings": [
  { "id": "enabled", "type": "toggle", "title": "启用记录", "default": true },
  { "id": "token", "type": "text", "title": "令牌" },
  { "id": "mode", "type": "select", "title": "模式", "options": [{ "value": "fast", "label": "快速" }, { "value": "safe" }] },
  { "id": "interval", "type": "number", "title": "间隔", "min": 1, "max": 60, "step": 1, "default": 30 }
]
```

| `type` | 说明 | 专属字段 |
|---|---|---|
| `toggle` | 开关 | |
| `text` | 文本，最长 500 个字符 | |
| `select` | 单选，最多 32 项 | `options`，每项 `value` 必需，`label` 可选 |
| `number` | 滑块 | `min`、`max` 必需，`step` 默认 1 |

- 设置值保存在插件数据目录，升级时保留，不占用 `storage` 权限。
- 没保存过时取 `default`，不合法的值回到默认值；未写 `default` 时，`toggle` 为 `false`、`text` 为空、`select` 为第一项、`number` 为 `min`。
- 如果注册了动作 `settingsChanged`，每次修改都会以 `{ key, value }` 调用一次。

## 播放事件

| 事件 | 数据 |
|---|---|
| `songChanged` | `{ id, songId, title, artist, cover }`，停止播放时为 `null` |
| `playStateChanged` | `{ playing }` |
| `progress` | `{ positionMs, durationMs }`，播放中每秒一次 |

事件只通知插件，不等待处理结果，也不能改变播放行为。插件启动时不会补发当前状态，需要时调用 `melodia.player.current()` 与 `melodia.player.state()`。

## 播放器布局

布局是由容器和组件组成的树，写在 `ui/*.json` 里，或直接内联在清单的 `playerLayouts` 下。只提供布局的插件不需要 `main.js`，也不需要申请权限。

```json
{
  "type": "row", "padding": [0, 16], "spacing": 12, "align": "center", "height": 96,
  "children": [
    { "type": "cover", "shape": "circle", "maxSize": 64 },
    { "type": "songInfo", "weight": 1 },
    { "type": "column", "weight": 2, "align": "center", "children": [
      { "type": "controls", "buttons": ["shuffle", "previous", "playPause", "next", "repeat"], "size": "large" },
      { "type": "progress", "showTime": false }
    ]},
    { "type": "actions", "buttons": ["queue", "volume"] }
  ]
}
```

### 外框与变体

API 1.5 起，区域的值也可以写成带 `layout` 字段的对象，用来调整宿主保留的部分，或按尺寸换一套布局。直接写布局树的旧写法等于只写了 `layout`。

```json
"android.fullPlayer": {
  "frame": { "header": "none", "cards": false },
  "layout": { "type": "column", "children": [] },
  "variants": [
    { "when": { "orientation": "landscape" }, "layout": { "type": "row", "children": [] } }
  ]
}
```

- `frame`：区域外框，各区域支持的字段见下表，写了不支持的字段会被忽略并提示。
- `variants`：最多 4 条，按顺序取第一条满足 `when` 的替换 `layout`，都不满足时用 `layout`。条件可写 `minWidth`、`maxWidth`（区域实际宽度，单位 dp）与 `orientation`（`portrait`、`landscape`）。每条单独校验，某条无效只跳过这一条。
- `styles`：具名样式，见下文「样式」（API 1.7）。
- 节点上的 `animation` 与 `onTap`、`onDoubleTap`、`onLongPress` 见下文「动效」「手势」（API 1.8）。

| 区域 | `frame` 字段 |
|---|---|
| `android.fullPlayer` | `header`：`host`（默认，保留顶部的收起、播放来源与更多）或 `none`（去掉，由布局自己摆 `collapseButton` 等组件）；`cards`：`false` 时去掉下方的信息卡片，首屏就是整页 |
| `android.fullPlayerWide` | `split`：左栏宽度占比，0.3–0.7，默认 0.4；左栏内容最宽 420 |
| `android.miniPlayer` | `background`：`host`（默认，封面取色）、`none`，或颜色、渐变（见「颜色」）；`radius`：一个数字，或 `[左上, 右上, 右下, 左下]`，默认 8；`shadow`、`border`：外框阴影与描边，默认 `true` |

`header` 为 `none` 而布局里没有一定看得见的 `collapseButton` 时，宿主会在左上角补一个收起按钮。放在 `pager` 里、写了 `visible` 条件、不透明度低于 0.5、宽高小于 16 或静止时被动效淡出、缩小的都不算看得见；`autoHide` 由宿主控制，照常算。必需组件按同样的规则判断，被这样隐藏的播放/暂停按钮或进度条不算数，整个布局无效。下拉收起与返回键始终可用。

### 区域

| key | 位置 | 必需 | 不可用的组件 |
|---|---|---|---|
| `android.fullPlayer` | Android 全屏播放页首屏 | `playPause` | |
| `android.fullPlayerWide` | Android 宽屏播放页 | `playPause` | `collapseButton`、`moreButton`、`sourceLabel` |
| `android.miniPlayer` | Android 迷你播放器 | `playPause` | `actions`、`pager`、`collapseButton`、`moreButton`、`sourceLabel` |

`android.miniPlayer` 根节点的 `height` 范围 48–140，`controls` 只支持 `previous`、`playPause`、`next`；左右滑动切歌仍然可用，但封面和标题不会跟手移动。

`android.fullPlayer` 占满一屏，默认顶部保留收起和更多按钮，下方的信息卡片不变。手机横屏仍使用这个区域，可以用 `orientation: landscape` 的变体单独排版。`android.fullPlayerWide` 只在平板侧栏铺开到全屏时出现，只替换左栏。

### 通用属性

| 属性 | 说明 |
|---|---|
| `padding` | 一个数字为四边相同；`[上下, 左右]`；`[上, 右, 下, 左]`。范围 0–64 |
| `spacing` | 容器内子节点间距，范围 0–64 |
| `align` | `start`、`center`、`end` |
| `weight` | 在 `row` / `column` 里按比例分配剩余空间，范围 0.1–10 |
| `width` / `height` | 固定尺寸，范围 0–1000；也可以写 `"45%"`，按父容器剩余空间的比例（API 1.5） |
| `minWidth` / `maxWidth` / `minHeight` / `maxHeight` | 尺寸上下限，范围 0–1000（API 1.5） |
| `arrange` | `row` / `column` 主轴排列：`start`、`center`、`end`、`spaceBetween`、`spaceAround`、`spaceEvenly`；后三种不使用 `spacing`（API 1.5）；`row` 写了 `start` 以外的排列又没写宽度时自动占满宽度（API 1.8） |
| `alignSelf` | 在 `box` 里的位置：`topStart`、`top`、`topEnd`、`start`、`center`、`end`、`bottomStart`、`bottom`、`bottomEnd`；在 `row` / `column` 里只取交叉轴方向（API 1.5） |
| `offset` | `[x, y]`，范围 -200–200，只移动绘制位置，不影响布局（API 1.5） |
| `background` | 颜色，如 `pane`、`cardSurface`；API 1.7 起也可以写其他颜色值或渐变，见「颜色」 |
| `contentColor` | 节点里文字与图标的颜色，次级、三级文字按透明度递减；宿主组件都会跟着变（API 1.7） |
| `opacity` | 0–1，整个节点的不透明度（API 1.7） |
| `elevation` | 0–24，按 `shape` 投影的阴影（API 1.7） |
| `border` | `{ "width": 1, "color": "outline" }`，宽度 0–8（API 1.7） |
| `textAlign` | `start`、`center`、`end`，用于 `text`、`time`（API 1.7） |
| `marquee` | `true` 时单行放不下的文字横向滚动，用于 `text`（API 1.9） |
| `autoHide` | `true` 时随播放状态自动淡出并停止接收点击，不改变布局（API 1.9） |
| `class` | 引用 `styles` 里的样式（API 1.7） |
| `shape` / `radius` | `rect`、`rounded`、`circle`；`radius` 为圆角半径，默认 8；API 1.5 起可以写 `[左上, 右上, 右下, 左下]` |

### 节点

| type | 说明 | 专属参数 |
|---|---|---|
| `column` / `row` / `box` | 容器，`box` 为叠放 | `children` |
| `pager` | 分页容器，左右滑动翻页，每个子节点一页，最多 5 页，不能嵌套；分页器里的封面不再左右滑动切歌（API 1.5） | `children`、`indicator`（`none`、`dots`、`bar`）、`initialPage` |
| `cover` | 封面，点击切换「正在播放」 | `maxSize`（24–1000）、`aspect`（0.25–4，仅 Android）、`size` |
| `songTitle` / `artist` / `songInfo` | 标题、歌手、两者组合 | `lines`（1–4）、`size` |
| `likeButton` | 收藏到歌单；播客为订阅电台 | |
| `progress` | 进度条 | `showTime` |
| `time` | 播放时间文字（Android API 1.6） | `format`：`position`（默认）、`duration`、`remaining`、`both`（如 0:12 / 3:45）；`style`、`size` |
| `controls` | 播放控制 | `buttons`：`shuffle`、`previous`、`playPause`、`next`、`repeat`、`playMode`（单键按列表循环、单曲循环、随机切换；Android API 1.6）；`size`；`variant`：`default`、`plain`（播放键也是无底色图标）、`outlined`（圆形描边），API 1.7，迷你播放器不支持 |
| `actions` | 操作按钮 | `buttons`：`device`、`queue`、`share`、`quality`、`more`，API 1.6 起另有 `sleepTimer`、`comments` |
| `spacer` | 留白，配合 `weight` 或固定尺寸 | |
| `text` | 固定文字，不超过 40 个字符 | `text`、`style`（`title`、`body`、`caption`）、`lines`、`size` |
| `collapseButton` | 收起按钮；侧栏模式下带全屏切换（Android 全屏页，API 1.5） | |
| `moreButton` | 更多（Android 全屏页，API 1.5） | |
| `sourceLabel` | 播放来源，点击打开来源歌单（Android 全屏页，API 1.5） | |

`size` 取 `small`、`medium`、`large`。没写 `buttons` 时使用该区域的全部按钮，但 API 1.6 新增的 `playMode`、`sleepTimer`、`comments` 需要写明才会出现。

### 播放状态绑定

API 1.6 起，播放器区域里 `text` 的文字和任意节点的 `visible` 可以写成 `$player.xxx`：

| 字段 | 值 |
|---|---|
| `title` / `artist` / `album` | 字符串 |
| `source` | 播放来源名称，如「每日推荐」、歌单名 |
| `quality` | 音质标签，如「无损」；用户关掉音质显示或播客时为空 |
| `playing` / `liked` / `isPodcast` | 布尔值 |
| `playMode` | `listLoop`、`singleLoop`、`shuffle` |

```json
{ "type": "text", "text": "$player.quality", "visible": "$player.quality" }
```

写了不存在的字段，或在播放器区域用 `$state` / `$item` 时，这一项会被忽略并提示。进度不开放绑定，用 `time` 组件显示。

### 颜色

API 1.7 起，`background`、`contentColor`、`border` 的颜色和渐变色都可以写：

| 写法 | 说明 |
|---|---|
| 色板项 | 如 `pane`、`textPrimary`、`accent`，跟随皮肤 |
| `cover.base` / `cover.accent` | 封面取色的底色与高亮色 |
| `"#RRGGBB"` / `"#AARRGGBB"` | 固定颜色，深浅色共用 |
| `{ "light": "#…", "dark": "#…" }` | 按深浅色分别取色 |

渐变写成 `{ "gradient": ["cover.base", "pane"], "angle": 90 }`，2–4 个颜色，线性；`angle` 为 0 时从左到右，90 时从上到下，默认 90。

### 样式

API 1.7 起，区域外壳可以写 `styles`，节点用 `class` 引用：

```json
{
  "styles": {
    "muted": { "contentColor": "textTertiary", "opacity": 0.8 },
    "accent": { "extends": "muted", "contentColor": { "light": "#2E8B57", "dark": "#4DAF7C" } }
  },
  "layout": { "type": "time", "class": "accent" }
}
```

- 样式就是一组节点属性，节点自己写的同名属性优先。不能写 `type`、`children`、`buttons`、`bind`、`action`、`visible`。
- `extends` 继承另一个样式，最多 4 层；出现循环或继承的样式不存在时，这个样式无效。
- 一个区域最多 32 个样式，`layout` 与各条变体共用；引用无效样式时忽略并提示。

### 动效

API 1.8 起，任意节点可以写 `animation`，只改变绘制，不影响布局；旋转、缩放后可能超出父容器，需要自己留出空间。`layout` 与每条变体各自最多 16 个节点带动效。用户关闭「界面动画」后，动效停在静止状态。

预设写成 `"animation": "rotateWhilePlaying"` 或 `{ "preset": "rotateWhilePlaying", "period": 12 }`：

| 预设 | 效果 | 参数 |
|---|---|---|
| `rotateWhilePlaying` | 播放时匀速旋转，暂停时停在当前角度 | `period`：转一圈的秒数，2–60，默认 20 |
| `scaleOnPause` | 暂停时缩小，播放时弹回 | `scale`：暂停时的比例，0.5–1，默认 0.85 |
| `pulse` | 播放时轻微呼吸缩放 | `period`：秒，0.5–10，默认 2；`scale`：最大比例，1–1.2，默认 1.04 |
| `songChange` | 切歌时进入 | `effect`：`fade`（默认）、`slideUp`、`zoom` |

关键帧：

```json
"animation": {
  "keyframes": [{ "at": 0, "rotate": -25 }, { "at": 1, "rotate": 0 }],
  "duration": 600, "easing": "easeOut", "repeat": "once", "trigger": "playing"
}
```

| 字段 | 说明 |
|---|---|
| `keyframes` | 2–8 帧，`at` 为 0–1 且递增；每帧可写 `scale`（0–3）、`rotate`（度）、`alpha`（0–1）、`x` / `y`（-200–200），没写的属性沿用上一帧 |
| `duration` | 毫秒，100–60000，默认 1000 |
| `easing` | `linear`、`ease`（默认）、`easeIn`、`easeOut` |
| `repeat` | `once`（默认）、`loop`、`reverse`（往返） |
| `trigger` | `always`（默认）；`playing`：开始播放时正放，暂停时倒放回第一帧；`songChange`：每次切歌播放一次；`tap`：点击这个节点时播放一次 |

`songChange` 与 `tap` 在触发前停在最后一帧。关闭界面动画时，只播一次的动效停在最后一帧，循环的停在第一帧，`playing` 按播放状态停在首帧或末帧。

### 手势

API 1.8 起，任意节点可以写 `onTap`、`onDoubleTap`、`onLongPress`。自带点击的组件（按钮、歌名等）先处理自己的点击，这时节点上的手势不触发；分页器的左右滑动、下拉收起与返回键不受影响。

| 动作 | 可用区域 |
|---|---|
| `togglePlay`、`next`、`previous`、`seekForward`、`seekBack`（10 秒）、`toggleLike`、`cyclePlayMode` | 全部 |
| `pagerNext`、`pagerPrevious` | 全部；让最近一层 `pager` 翻页，不在分页器里时无效 |
| `openQueue`、`openComments`、`openSleepTimer`、`openMore` | Android 全屏页、宽屏页 |
| `collapse` | Android 全屏页 |
| `script:<名称>` | 全部；调用 `main.js` 里 `melodia.register.action` 注册的同名动作，参数为当前播放状态（字段同 `$player`），5 秒内没有返回按失败处理 |

区域不支持的动作会被忽略并提示。插件用 `script:` 动作时，要在清单的 `contributes` 里写 `"layoutActions": true` 并带上 `main.js`，否则这些动作会被忽略。

### 校验与回退

- 树不超过 8 层、128 个节点，`layout` 与每条变体分别计算；超限或缺少必需组件时，这个区域回到内置布局，其他区域不受影响。
- 未知组件、不支持的按钮与无效参数只忽略这一项，并在导入时提示；数值超出范围时收到边界内。
- 同一区域有多个插件提供布局时，默认使用最近启用的插件，也可以在插件设置里按区域选择，或改回内置布局。

## 页面

页面描述的写法与播放器布局相同，可用容器 `column`、`row`、`box`，以及 `spacer`、`text` 和下面的组件。提供页面的插件需要 `main.js`。

```json
{
  "type": "column", "spacing": 12, "children": [
    { "type": "heading", "text": "$state.title" },
    { "type": "loading", "visible": "$state.loading" },
    { "type": "button", "text": "刷新", "action": "refresh" },
    { "type": "list", "bind": "$state.items", "spacing": 6, "children": [
      { "type": "row", "children": [
        { "type": "text", "text": "$item.name", "weight": 1 },
        { "type": "button", "text": "删除", "action": "remove" }
      ]}
    ]}
  ]
}
```

| type | 说明 | 必需字段 |
|---|---|---|
| `heading` | 标题 | `text` |
| `text` | 正文，可绑定 | `text` |
| `image` | 图片，`bind` 可写绑定或地址；只允许 `assets/` 下的文件与 `network.hosts` 里的域名 | `bind` |
| `list` | 绑定数组，每一项按 `children` 渲染，最多 200 项；`columns`（1–8）大于 1 时按网格排列 | `bind` |
| `trackList` | 绑定 `{ songId, title, artist, cover }` 数组，点击从这首开始播放整个列表；需要 `playback.control` | `bind` |
| `button` | 触发 `action`；在列表里时，当前项作为参数传给脚本，否则传点击时的页面数据（含输入框里还没提交的内容，API 1.4 起） | `text`、`action` |
| `toggle` | 开关，写回 `$state` 的第一层字段（`input`、`select`、`slider` 相同）；有 `action` 时以 `{ value }` 调用 | `text`、`bind` |
| `input` | 输入框，写回 `$state` 字段；有 `action` 时在回车后以 `{ value }` 调用；`secret` 为 `true` 时遮挡显示（API 1.4 起） | `text`（提示文字）、`bind` |
| `loading` / `empty` | 加载中与空态 | |
| `select` | 单选，写回 `$state` 字段；有 `action` 时以 `{ value }` 调用 | `text`（标题）、`bind`、`options` |
| `slider` | 滑块，写回 `$state` 字段；松手后调用 `action` | `text`（标题）、`bind`；`min`、`max` 默认 0、1，`step` 可选 |
| `progressBar` | 进度条，绑定 0–1 的数字 | `bind` |
| `badge` | 小标签 | `text` |
| `tabs` | 标签页，每个子节点是一页，子节点的 `text` 为标题 | `children` |

- **绑定**：`$state.xxx` 取页面数据，`$item.xxx` 取列表当前项，可以用点号取下一层。`text` 可以写固定文字，也可以整段写成绑定。
- **`visible`**：任何节点都可以写，值为绑定，取到空、`false`、`0` 或空数组时不显示。
- **数据**：脚本用 `melodia.ui.setState(pageId, patch)` 更新页面数据，渲染时不会调用脚本。
- **动作**：用 `melodia.register.action(name, handler)` 注册，名称只能用字母、数字、`_`、`:`、`-`，5 秒内没有返回按失败处理。
- **打开页面**：如果注册了 `open:<页面 id>`，每次打开页面都会调用一次，可以用来刷新数据。
- **入口**：`placement` 为 `sidebar` 时，在「设置 → 扩展 → 插件」里打开；为 `nowPlayingCard` 时，页面作为卡片显示在全屏播放页，可以在卡片编辑器里排序和隐藏，宽屏播放页的插件卡片排在内置卡片之后。为 `settings` 时，页面不进侧栏，插件列表里这个插件会出现设置按钮，点开后在设置弹窗里显示页面；同时声明了 `settings` 设置项时，设置项排在页面下方。每个插件只取第一个 `settings` 页面。

## 分发

除了手动导入，还可以提交到插件目录，见 [plugin-catalog.md](plugin-catalog.md)。升级时保留上一版本，可以在插件列表里回退，插件数据不变。

## 存储

- 已安装的插件放在应用数据目录的 `plugins/<id>/` 下，启用状态与已确认的权限记录在 `plugins/.state.json`。
- 插件数据放在 `plugins/.data/<id>/`，升级时保留，删除插件时一并删除。
- 导出时只打包安装文件，不包含插件数据。

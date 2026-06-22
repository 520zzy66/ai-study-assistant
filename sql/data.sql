-- AI 学习助手初始化数据

USE ai_study;
SET NAMES utf8mb4;

-- 插入测试用户 (密码: 123456, BCrypt加密)
-- BCrypt hash for "123456" with strength=10
INSERT INTO user (username, password, nickname, email) VALUES
('testuser', '$2a$10$EqKcp1WFKVQIShMPC7B3kuznX9gAZMsVnSNjN0SHiXh1bXbLxKO3e', '测试用户', 'test@example.com')
ON DUPLICATE KEY UPDATE username=username;

-- ============================================================
-- 系统预置资料（资料库）
-- user_id = 0, source = 'system', status = 'ready'
-- ============================================================

INSERT INTO learning_material (user_id, original_name, stored_name, file_type, file_size, file_path, category, summary, status, source, chunk_count) VALUES
(0, '费曼学习法.txt', 'system_feynman.txt', 'txt', 2048, '', '学习方法', '费曼学习法是一种以教促学的高效学习方法。核心理念：如果你不能用简单的语言解释一个概念，说明你还没有真正理解它。步骤：选择概念→假装教人→发现盲区→简化回顾。', 'ready', 'system', 3),
(0, '番茄工作法.txt', 'system_pomodoro.txt', 'txt', 1536, '', '学习方法', '番茄工作法是一种时间管理方法，通过25分钟专注+5分钟休息的循环来提高学习效率。核心要点：设定任务→25分钟专注→短暂休息→每4个番茄长休息。', 'ready', 'system', 3),
(0, '数据结构与算法基础.txt', 'system_ds_algo.txt', 'txt', 3072, '', '计算机基础', '数据结构是计算机存储和组织数据的方式。常见数据结构：数组、链表、栈、队列、树、图、哈希表。算法复杂度用大O表示法衡量。', 'ready', 'system', 4),
(0, '计算机网络基础.txt', 'system_network.txt', 'txt', 2560, '', '计算机基础', '计算机网络是通过通信设备和线路连接多台计算机的系统。OSI七层模型和TCP/IP四层模型是理解网络通信的基础框架。', 'ready', 'system', 3),
(0, '英语四六级高频词汇.txt', 'system_cet_vocab.txt', 'txt', 2048, '', '英语学习', '整理了英语四六级考试中出现频率最高的核心词汇，涵盖动词、名词、形容词等词性，配有例句和记忆技巧。', 'ready', 'system', 3),
(0, '英语阅读理解技巧.txt', 'system_reading.txt', 'txt', 1792, '', '英语学习', '英语阅读理解是考试中的重点题型。掌握略读、扫读、精读三种阅读技巧，以及主旨题、细节题、推断题的解题方法，能显著提高阅读得分。', 'ready', 'system', 3);

-- ============================================================
-- 系统预置资料的切片数据
-- ============================================================

-- 费曼学习法 切片
INSERT INTO material_chunk (material_id, user_id, chunk_index, content, chunk_size, deleted) VALUES
((SELECT id FROM learning_material WHERE original_name='费曼学习法.txt' AND source='system' LIMIT 1), 0, 0,
'费曼学习法（Feynman Technique）是由诺贝尔物理学奖得主理查德·费曼命名的一种高效学习方法。其核心理念是：如果你不能用简单的语言向别人解释一个概念，说明你还没有真正理解它。这种方法强调"以教促学"，通过尝试将复杂的知识用最简单的语言表达出来，来检验和加深自己的理解。

费曼学习法的四个步骤：
第一步：选择一个你想学习的概念或主题。
第二步：假装你在向一个完全不懂这个领域的人（比如小学生）讲解这个概念。用最简单的语言，避免使用专业术语。
第三步：在讲解过程中，你会发现有些地方自己说不清楚或者卡壳了，这些就是你的知识盲区。回到原始资料，重新学习这些部分。
第四步：简化和回顾。用更简洁、更直白的语言重新组织你的解释，直到你能流畅地讲清楚整个概念。', 580, 0),

((SELECT id FROM learning_material WHERE original_name='费曼学习法.txt' AND source='system' LIMIT 1), 0, 1,
'费曼学习法的实际应用场景：

1. 课堂笔记整理：听完课后，尝试用自己的话把老师讲的内容重新写一遍，而不是照抄板书。如果你写不出来，说明这部分没有听懂。

2. 考试复习：把每个知识点当作要教给别人的内容，先不看教材，尝试自己讲一遍。讲不清楚的地方就是需要重点复习的地方。

3. 编程学习：学了一个新的编程概念后，试着向一个不懂编程的朋友解释它是怎么工作的。比如解释"递归"：就像你站在两面镜子中间，看到无限的自己，每次看到的都是同一个画面但更小一点。

4. 团队分享：定期在学习小组中做分享，把你学到的知识教给别人。教是最好的学。', 420, 0),

((SELECT id FROM learning_material WHERE original_name='费曼学习法.txt' AND source='system' LIMIT 1), 0, 2,
'费曼学习法与其他学习方法的对比：

与传统死记硬背相比，费曼学习法更注重理解。死记硬背只能短期记住，而费曼法通过深度理解形成的知识可以长期保持。

与思维导图相比，费曼法更强调输出。思维导图是整理知识结构，费曼法是检验你是否真正掌握了知识。

与做题练习相比，费曼法更侧重概念理解。做题是检验应用能力，费曼法是检验理解深度。两者结合使用效果最佳。

使用费曼学习法的注意事项：
- 不要跳过"用自己的话解释"这一步，光看懂不等于理解
- 遇到卡壳不要急着看答案，先尝试自己推理
- 解释时尽量用类比和生活中的例子
- 定期回顾，防止遗忘', 380, 0);

-- 番茄工作法 切片
INSERT INTO material_chunk (material_id, user_id, chunk_index, content, chunk_size, deleted) VALUES
((SELECT id FROM learning_material WHERE original_name='番茄工作法.txt' AND source='system' LIMIT 1), 0, 0,
'番茄工作法（Pomodoro Technique）是由意大利人弗朗西斯科·西里洛在20世纪80年代末发明的一种时间管理方法。名字来源于他使用的番茄形状的厨房定时器。

核心原理：人的注意力集中时间是有限的，通常在25分钟左右就会开始下降。通过将工作分割成25分钟的专注时间段（称为一个"番茄钟"），中间穿插短暂休息，可以保持高效的学习状态。

基本步骤：
1. 选择一个要完成的任务
2. 设定25分钟的计时器
3. 专注工作直到计时器响起
4. 短暂休息5分钟
5. 每完成4个番茄钟后，休息15-30分钟', 480, 0),

((SELECT id FROM learning_material WHERE original_name='番茄工作法.txt' AND source='system' LIMIT 1), 0, 1,
'番茄工作法的使用技巧：

1. 任务分解：如果一个任务超过4个番茄钟，应该将其分解为更小的子任务。比如"复习数学"可以分解为"复习微积分第1章"、"做练习题1-10"等。

2. 专注原则：在番茄钟期间，不允许任何中断。如果突然想到其他事情，记在纸上，等休息时间再处理。手机静音、关闭社交媒体通知。

3. 记录追踪：每天记录完成了多少个番茄钟，分别花在了什么任务上。这样可以清楚地看到自己的时间都去了哪里。

4. 灵活调整：25分钟不是固定的。有些人适合30分钟或20分钟。找到最适合自己的时长。', 420, 0),

((SELECT id FROM learning_material WHERE original_name='番茄工作法.txt' AND source='system' LIMIT 1), 0, 2,
'番茄工作法在学习中的应用：

数学学习：一个番茄钟做一套练习题，休息时回顾错题。连续3-4个番茄钟后做一次总结。

编程练习：一个番茄钟专注写代码，遇到bug不要急，记录下来。休息时间再回头解决。避免在一个bug上浪费整个番茄钟。

英语学习：一个番茄钟背单词，一个番茄钟做阅读，交替进行可以防止疲劳。

论文写作：番茄钟期间只写不改，把修改放在专门的番茄钟里。先完成再完美。

常见问题：
- 总被打断怎么办？告知周围的人你在专注时间，设置专门的"可打扰时间"
- 25分钟太短？对于深度思考的任务，可以尝试50分钟+10分钟的长番茄钟
- 休息时做什么？离开座位、喝水、伸展身体，不要刷手机', 400, 0);

-- 数据结构与算法基础 切片
INSERT INTO material_chunk (material_id, user_id, chunk_index, content, chunk_size, deleted) VALUES
((SELECT id FROM learning_material WHERE original_name='数据结构与算法基础.txt' AND source='system' LIMIT 1), 0, 0,
'数据结构是计算机科学的基础，它定义了数据的组织、管理和存储方式。选择合适的数据结构可以极大地提高算法的效率。

线性数据结构：
1. 数组（Array）：连续内存空间存储相同类型元素，支持O(1)随机访问，但插入删除需要O(n)移动元素。适用于需要频繁按索引访问的场景。

2. 链表（Linked List）：通过指针将节点串联，插入删除O(1)（已知位置时），但访问需要O(n)遍历。适用于频繁插入删除的场景。

3. 栈（Stack）：后进先出（LIFO），只能在一端操作。应用场景：函数调用栈、括号匹配、表达式求值、浏览器后退。

4. 队列（Queue）：先进先出（FIFO），一端入队一端出队。应用场景：任务调度、广度优先搜索、消息队列。', 450, 0),

((SELECT id FROM learning_material WHERE original_name='数据结构与算法基础.txt' AND source='system' LIMIT 1), 0, 1,
'非线性数据结构：

1. 树（Tree）：层级结构，最常见的是二叉树。二叉搜索树（BST）支持O(log n)的查找、插入、删除。应用场景：文件系统、数据库索引、表达式树。

2. 堆（Heap）：完全二叉树，最大堆保证父节点大于子节点。应用场景：优先队列、堆排序、Top-K问题。

3. 图（Graph）：由顶点和边组成，分为有向图和无向图。表示方法：邻接矩阵（稠密图）和邻接表（稀疏图）。应用场景：社交网络、路径规划、网络拓扑。

4. 哈希表（Hash Table）：通过哈希函数将键映射到数组索引，平均O(1)的增删查。冲突解决：链地址法、开放定址法。应用场景：缓存、去重、计数器。', 420, 0),

((SELECT id FROM learning_material WHERE original_name='数据结构与算法基础.txt' AND source='system' LIMIT 1), 0, 2,
'算法复杂度分析：

时间复杂度衡量算法执行时间随输入规模增长的变化趋势。常见复杂度从低到高：
- O(1)：常数时间，如数组按索引访问
- O(log n)：对数时间，如二分查找
- O(n)：线性时间，如遍历数组
- O(n log n)：线性对数时间，如归并排序、快速排序
- O(n²)：平方时间，如冒泡排序
- O(2ⁿ)：指数时间，如递归计算斐波那契

空间复杂度衡量算法额外使用的内存空间。比如归并排序需要O(n)额外空间，而快速排序只需要O(log n)的栈空间。

实际应用建议：
- 数据量小（n<100）时，复杂度差异不大，代码简洁更重要
- 数据量大时，选择合适的算法至关重要
- 时间和空间往往需要权衡，用空间换时间是常见策略', 460, 0),

((SELECT id FROM learning_material WHERE original_name='数据结构与算法基础.txt' AND source='system' LIMIT 1), 0, 3,
'常用排序算法对比：

冒泡排序：O(n²)，稳定，适合小数据量教学演示。
选择排序：O(n²)，不稳定，简单但效率低。
插入排序：O(n²)，稳定，对近乎有序的数据表现很好。
归并排序：O(n log n)，稳定，需要额外空间，适合链表排序。
快速排序：O(n log n)平均，不稳定，实际应用最广泛的排序算法。
堆排序：O(n log n)，不稳定，原地排序不需要额外空间。

学习建议：
1. 先理解每种数据结构的特点和适用场景
2. 手写代码实现每种数据结构的基本操作
3. 做LeetCode上对应标签的题目练习
4. 学会分析时间和空间复杂度
5. 理解何时该用什么数据结构，这比记住实现细节更重要', 380, 0);

-- 计算机网络基础 切片
INSERT INTO material_chunk (material_id, user_id, chunk_index, content, chunk_size, deleted) VALUES
((SELECT id FROM learning_material WHERE original_name='计算机网络基础.txt' AND source='system' LIMIT 1), 0, 0,
'计算机网络是将多台计算机通过通信设备和传输介质连接起来，实现资源共享和信息传递的系统。

OSI七层模型（从下到上）：
1. 物理层：负责比特流的传输，定义电压、接口等物理特性
2. 数据链路层：负责帧的传输，提供差错检测，如以太网协议
3. 网络层：负责数据包的路由和转发，IP协议工作在这一层
4. 传输层：负责端到端的可靠传输，TCP和UDP协议工作在这一层
5. 会话层：管理会话建立和维护
6. 表示层：数据格式转换、加密解密
7. 应用层：为用户应用提供网络服务，HTTP、FTP、SMTP等协议工作在这一层

TCP/IP四层模型是实际使用的简化版本：网络接口层、网际层、传输层、应用层。', 480, 0),

((SELECT id FROM learning_material WHERE original_name='计算机网络基础.txt' AND source='system' LIMIT 1), 0, 1,
'TCP协议（传输控制协议）：

TCP是面向连接的可靠传输协议，三次握手建立连接：
客户端→服务器：SYN（我想连接）
服务器→客户端：SYN+ACK（同意连接）
客户端→服务器：ACK（确认，连接建立）

四次挥手断开连接：
主动方→被动方：FIN（我要关闭）
被动方→主动方：ACK（收到，等我处理完）
被动方→主动方：FIN（我也关闭）
主动方→被动方：ACK（确认关闭）

TCP通过序列号、确认应答、超时重传、流量控制（滑动窗口）、拥塞控制等机制保证可靠传输。

UDP协议（用户数据报协议）：
UDP是无连接的不可靠传输协议，开销小、速度快。适用于对实时性要求高但允许少量丢包的场景，如视频直播、在线游戏、DNS查询。', 450, 0),

((SELECT id FROM learning_material WHERE original_name='计算机网络基础.txt' AND source='system' LIMIT 1), 0, 2,
'HTTP协议（超文本传输协议）：

HTTP是应用层协议，用于Web浏览器和服务器之间的通信。基于请求-响应模型。

常见状态码：
200 OK：请求成功
301 Moved Permanently：永久重定向
302 Found：临时重定向
304 Not Modified：资源未修改，使用缓存
400 Bad Request：请求语法错误
401 Unauthorized：需要身份认证
403 Forbidden：服务器拒绝请求
404 Not Found：资源不存在
500 Internal Server Error：服务器内部错误

HTTPS = HTTP + TLS/SSL，在HTTP基础上增加加密和身份验证。使用非对称加密交换密钥，对称加密传输数据。

学习建议：理解TCP/IP和HTTP是后端开发的基础，建议配合Wireshark抓包工具实际观察网络数据包。', 420, 0);

-- 英语四六级高频词汇 切片
INSERT INTO material_chunk (material_id, user_id, chunk_index, content, chunk_size, deleted) VALUES
((SELECT id FROM learning_material WHERE original_name='英语四六级高频词汇.txt' AND source='system' LIMIT 1), 0, 0,
'四六级高频动词词汇：

1. abandon /əˈbændən/ v. 放弃，抛弃
   例句：He abandoned his plan to travel abroad. 他放弃了出国旅行的计划。
   记忆：a+band+on → 一个乐队在演出中被抛弃

2. absorb /əbˈsɔːrb/ v. 吸收；全神贯注
   例句：Plants absorb nutrients from the soil. 植物从土壤中吸收养分。
   搭配：be absorbed in 全神贯注于

3. accelerate /əkˈseləreɪt/ v. 加速
   例句：The car accelerated quickly. 汽车迅速加速。
   记忆：ac+celer(快速)+ate → 使快速

4. accomplish /əˈkɑːmplɪʃ/ v. 完成，实现
   例句：She accomplished her goal of running a marathon. 她完成了跑马拉松的目标。

5. acknowledge /əkˈnɑːlɪdʒ/ v. 承认；感谢
   例句：He acknowledged his mistake. 他承认了自己的错误。', 420, 0),

((SELECT id FROM learning_material WHERE original_name='英语四六级高频词汇.txt' AND source='system' LIMIT 1), 0, 1,
'四六级高频名词词汇：

1. phenomenon /fəˈnɑːmɪnən/ n. 现象
   例句：Global warming is a serious phenomenon. 全球变暖是一个严重的现象。
   复数：phenomena

2. perspective /pərˈspektɪv/ n. 观点，视角
   例句：Try to see things from a different perspective. 试着从不同的视角看问题。

3. priority /praɪˈɔːrəti/ n. 优先事项
   例句：Education should be a top priority. 教育应该是首要优先事项。
   搭配：give priority to 优先考虑

4. responsibility /rɪˌspɑːnsəˈbɪləti/ n. 责任
   例句：It is our responsibility to protect the environment. 保护环境是我们的责任。

5. strategy /ˈstrætədʒi/ n. 策略，战略
   例句：We need a new marketing strategy. 我们需要一个新的营销策略。', 400, 0),

((SELECT id FROM learning_material WHERE original_name='英语四六级高频词汇.txt' AND source='system' LIMIT 1), 0, 2,
'四六级高频形容词和副词：

1. essential /ɪˈsenʃəl/ adj. 必不可少的
   例句：Water is essential for life. 水是生命必不可少的。

2. significant /sɪɡˈnɪfɪkənt/ adj. 重要的，显著的
   例句：There has been a significant increase in sales. 销售额有了显著增长。

3. approximately /əˈprɑːksɪmətli/ adv. 大约
   例句：The meeting will last approximately two hours. 会议大约持续两小时。

4. inevitably /ɪnˈevɪtəbli/ adv. 不可避免地
   例句：Technology will inevitably change our lives. 科技将不可避免地改变我们的生活。

5. increasingly /ɪnˈkriːsɪŋli/ adv. 越来越多地
   例句：People are increasingly concerned about health. 人们越来越关注健康。

词汇记忆技巧：
- 词根词缀法：如 pre(前)+dict(说)=predict(预测)
- 语境记忆：把单词放在句子中记忆
- 间隔重复：按照遗忘曲线复习（1天、3天、7天、15天）', 380, 0);

-- 英语阅读理解技巧 切片
INSERT INTO material_chunk (material_id, user_id, chunk_index, content, chunk_size, deleted) VALUES
((SELECT id FROM learning_material WHERE original_name='英语阅读理解技巧.txt' AND source='system' LIMIT 1), 0, 0,
'英语阅读理解三种基本阅读方法：

1. 略读（Skimming）—— 抓主旨
目的：快速了解文章大意和结构
方法：只读标题、首段、每段首句和末段
时间：2-3分钟完成一篇
适用：回答"What is the main idea?"类问题

2. 扫读（Scanning）—— 找细节
目的：快速定位特定信息
方法：带着问题找关键词，眼睛快速扫过
技巧：先看题目，划出关键词（数字、人名、大写字母），再回文章定位
适用：回答细节题和事实题

3. 精读（Intensive Reading）—— 深理解
目的：理解复杂句、推断隐含意思
方法：逐句分析，关注转折词（but, however, yet）
技巧：遇到长句先找主谓宾，忽略修饰成分
适用：回答推理题和作者态度题', 480, 0),

((SELECT id FROM learning_material WHERE original_name='英语阅读理解技巧.txt' AND source='system' LIMIT 1), 0, 1,
'阅读理解四大题型及解题方法：

一、主旨题
标志词：main idea, mainly about, best title, purpose
方法：看首段+各段首句+末段，概括全文中心思想
注意：选项太具体（只涉及某段）或太宽泛（超出文章范围）都不对

二、细节题
标志词：according to, which is true/false, because
方法：根据题干关键词回原文定位，答案通常是原文的同义改写
注意：注意题目中的NOT/EXCEPT，避免看错题

三、推断题
标志词：infer, imply, suggest, conclude
方法：基于原文信息进行合理推断，答案不会直接出现在原文中
注意：推断要有依据，不能过度推测

四、词义猜测题
标志词：the underlined word means, refers to
方法：看上下文语境，利用同义词、反义词、举例等线索推断
注意：代词指代题要往前找最近的名词', 400, 0),

((SELECT id FROM learning_material WHERE original_name='英语阅读理解技巧.txt' AND source='system' LIMIT 1), 0, 2,
'阅读理解高分策略：

时间分配：
- 四级阅读共40分钟，建议每篇10分钟
- 六级阅读共40分钟，建议每篇8-10分钟
- 先做自己擅长的题型

做题顺序：
1. 先看题目（不看选项），划出关键词
2. 带着问题读文章，边读边标记相关信息
3. 回到题目，对照原文选择答案
4. 不确定的题目先跳过，最后回来处理

常见干扰项特征：
- 偷换概念：把A的特征说成B的
- 以偏概全：用局部信息代替整体
- 无中生有：文章没有提到的信息
- 过度推断：推断超出了原文的范围

日常练习建议：
- 每天坚持阅读英文文章（BBC、VOA、经济学人）
- 积累高频词汇和常见句型
- 做真题时计时训练，培养时间感
- 错题要分析原因，总结规律', 380, 0);

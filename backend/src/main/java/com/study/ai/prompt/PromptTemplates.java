package com.study.ai.prompt;

import com.study.dto.request.QaRequest;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI 提示词模板
 * 使用 Spring AI PromptTemplate 安全渲染变量
 */
public class PromptTemplates {

    private PromptTemplates() {
        // 工具类，禁止实例化
    }

    /**
     * 文档总结提示词模板
     */
    private static final String SUMMARY_TEMPLATE = """
            你是一个专业的学习助手。请根据以下学习资料内容，生成简洁易懂的知识点总结。

            要求：
            1. 提炼核心概念和重点，去除冗余信息
            2. 使用 Markdown 格式，层次清晰
            3. 按知识点分类，使用 ### 二级标题
            4. 每个知识点配 1-2 句简要说明
            5. 末尾附"复习建议"小节
            6. 总字数控制在 800-1500 字

            文档内容：
            {content}
            """;

    /**
     * RAG 文档问答提示词模板
     */
    private static final String QA_TEMPLATE = """
            你是一个专业的学习助手。根据以下参考资料回答用户问题。

            参考资料：
            {context}

            用户问题：{question}

            回答要求：
            1. 优先基于参考资料回答，引用具体段落
            2. 如果资料中没有相关内容，明确说明"资料中未找到相关内容"
            3. 回答准确、简洁、有条理
            4. 使用 Markdown 格式
            5. 如有必要，可补充通用知识，但需标注"[补充知识]"
            """;

    /**
     * 自动出题提示词模板
     */
    private static final String QUIZ_TEMPLATE = """
            你是一个专业的出题专家。根据以下学习资料生成练习题。

            文档内容：
            {content}

            出题要求：
            1. 生成 {choiceCount} 道单选题，选项 A/B/C/D
            2. 生成 {judgeCount} 道判断题，答案为 true/false
            3. 生成 {shortAnswerCount} 道简答题
            4. 难度等级：{difficulty}
            5. 每题附带正确答案和详细解析
            6. 题目必须基于文档内容，不得编造
            7. 选项分布均匀（答案不全是 C）

            严格返回以下 JSON 格式（不要包含 markdown 代码块标记）：
            \\{
              "questions": [
                \\{
                  "type": "choice",
                  "difficulty": "medium",
                  "question": "题干",
                  "options": \\{"A": "选项A", "B": "选项B", "C": "选项C", "D": "选项D"\\},
                  "answer": "C",
                  "explanation": "解析"
                \\},
                \\{
                  "type": "judge",
                  "difficulty": "easy",
                  "question": "题干",
                  "answer": "false",
                  "explanation": "解析"
                \\},
                \\{
                  "type": "short_answer",
                  "difficulty": "hard",
                  "question": "题干",
                  "answer": "参考答案",
                  "explanation": "解析"
                \\}
              ]
            \\}
            """;

    /**
     * 学习计划生成提示词模板
     */
    private static final String PLAN_TEMPLATE = """
            你是一个专业的学习规划师。请根据以下信息制定个性化学习计划。

            学习目标：{goal}
            考试日期：{examDate}
            今天日期：{today}
            每天可用时间：{dailyHours} 小时
            关联资料：{materialNames}

            要求：
            1. 从今天到考试日期按天规划
            2. 合理分配知识点，循序渐进，先基础后综合
            3. 每天任务量与可用时间匹配
            4. 包含复习日和模拟测试日
            5. 最后一周为冲刺复习

            严格返回以下 JSON 格式：
            \\{
              "plan": [
                \\{
                  "day": 1,
                  "date": "2026-06-17",
                  "weekday": "周三",
                  "topics": ["主题1", "主题2"],
                  "duration": "2小时",
                  "tasks": "具体任务描述",
                  "materials": ["资料名"]
                \\}
              ]
            \\}
            """;

    /**
     * 简答题判分提示词模板
     */
    private static final String GRADING_TEMPLATE = """
            你是一个严格的阅卷老师。请判断学生的答案是否正确。

            题目：{question}
            参考答案：{referenceAnswer}
            学生答案：{studentAnswer}

            评分要求：
            1. 核心概念一致即可得分，不要求逐字匹配
            2. 返回 JSON：\\{"score": 0.8, "feedback": "缺少循环等待条件的说明"\\}
            """;

    /**
     * 构建文档总结提示词
     *
     * @param content 文档内容
     * @return 提示词
     */
    public static String buildSummaryPrompt(String content) {
        return render(SUMMARY_TEMPLATE, Map.of("content", (Object) content));
    }

    /**
     * 通用对话提示词模板（无需学习资料）
     */
    private static final String GENERAL_CHAT_TEMPLATE = """
            你是一个智能学习助手，可以回答各类学习相关的问题。

            用户问题：{question}

            回答要求：
            1. 回答准确、简洁、有条理
            2. 使用 Markdown 格式
            3. 如果问题涉及专业知识，请确保信息准确可靠
            4. 如果不确定或超出知识范围，请诚实说明
            """;

    /**
     * 构建 RAG 问答提示词
     *
     * @param context  参考资料
     * @param question 用户问题
     * @return 提示词
     */
    public static String buildQaPrompt(String context, String question) {
        return render(QA_TEMPLATE, Map.of("context", (Object) context, "question", question));
    }

    /**
     * 构建通用对话提示词（无需学习资料）
     *
     * @param question 用户问题
     * @return 提示词
     */
    public static String buildGeneralChatPrompt(String question) {
        return render(GENERAL_CHAT_TEMPLATE, Map.of("question", question));
    }

    /**
     * 构建出题提示词
     *
     * @param content           文档内容
     * @param choiceCount       单选题数量
     * @param judgeCount        判断题数量
     * @param shortAnswerCount  简答题数量
     * @param difficulty        难度
     * @return 提示词
     */
    public static String buildQuizPrompt(String content, int choiceCount, int judgeCount,
                                          int shortAnswerCount, String difficulty) {
        return render(QUIZ_TEMPLATE, Map.of(
                "content", (Object) content,
                "choiceCount", (Object) String.valueOf(choiceCount),
                "judgeCount", (Object) String.valueOf(judgeCount),
                "shortAnswerCount", (Object) String.valueOf(shortAnswerCount),
                "difficulty", difficulty
        ));
    }

    /**
     * 构建学习计划提示词
     *
     * @param goal         学习目标
     * @param examDate     考试日期
     * @param today        今天日期
     * @param dailyHours   每天可用时间
     * @param materialNames 关联资料
     * @return 提示词
     */
    public static String buildPlanPrompt(String goal, String examDate, String today,
                                          int dailyHours, String materialNames) {
        return render(PLAN_TEMPLATE, Map.of(
                "goal", (Object) goal,
                "examDate", examDate,
                "today", today,
                "dailyHours", (Object) String.valueOf(dailyHours),
                "materialNames", materialNames
        ));
    }

    /**
     * 构建判分提示词
     *
     * @param question       题目
     * @param referenceAnswer 参考答案
     * @param studentAnswer  学生答案
     * @return 提示词
     */
    public static String buildGradingPrompt(String question, String referenceAnswer,
                                             String studentAnswer) {
        return render(GRADING_TEMPLATE, Map.of(
                "question", (Object) question,
                "referenceAnswer", referenceAnswer,
                "studentAnswer", studentAnswer
        ));
    }

    /**
     * 构建带历史对话的 RAG 问答提示词
     *
     * @param context  参考资料
     * @param question 用户问题
     * @param history  对话历史
     * @return 提示词
     */
    public static String buildQaPromptWithHistory(String context, String question,
                                                   List<QaRequest.ChatMessage> history) {
        String historyText = formatHistory(history);
        String template = """
                你是一个专业的学习助手。根据以下参考资料和对话历史回答用户问题。

                参考资料：
                {context}

                对话历史：
                {history}

                用户问题：{question}

                回答要求：
                1. 优先基于参考资料回答，引用具体段落
                2. 如果资料中没有相关内容，明确说明"资料中未找到相关内容"
                3. 回答准确、简洁、有条理
                4. 使用 Markdown 格式
                5. 如有必要，可补充通用知识，但需标注"[补充知识]"
                6. 注意上下文连贯性，理解代词指代（如"它"、"这个"等）
                """;
        return render(template, Map.of(
                "context", (Object) context,
                "history", historyText,
                "question", question
        ));
    }

    /**
     * 构建带历史对话的通用对话提示词
     *
     * @param question 用户问题
     * @param history  对话历史
     * @return 提示词
     */
    public static String buildGeneralChatPromptWithHistory(String question,
                                                            List<QaRequest.ChatMessage> history) {
        String historyText = formatHistory(history);
        String template = """
                你是一个智能学习助手，可以回答各类学习相关的问题。

                对话历史：
                {history}

                用户问题：{question}

                回答要求：
                1. 回答准确、简洁、有条理
                2. 使用 Markdown 格式
                3. 如果问题涉及专业知识，请确保信息准确可靠
                4. 如果不确定或超出知识范围，请诚实说明
                5. 注意上下文连贯性，理解代词指代（如"它"、"这个"等）
                """;
        return render(template, Map.of(
                "history", (Object) historyText,
                "question", question
        ));
    }

    /**
     * 格式化对话历史为文本
     */
    private static String formatHistory(List<QaRequest.ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "（无历史对话）";
        }
        StringBuilder sb = new StringBuilder();
        for (QaRequest.ChatMessage msg : history) {
            String role = "user".equals(msg.getRole()) ? "用户" : "助手";
            sb.append(role).append("：").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 渲染模板
     * 使用 Spring AI PromptTemplate，安全处理内容中的特殊字符（如 {}）
     *
     * @param template 模板
     * @param params   参数
     * @return 渲染后的字符串
     */
    private static String render(String template, Map<String, Object> params) {
        PromptTemplate pt = new PromptTemplate(template);
        return pt.create(params).getContents();
    }
}

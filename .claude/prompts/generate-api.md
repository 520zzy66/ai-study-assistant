# 生成 API

请根据 docs/spec.md 中对应模块设计，生成完整的 RESTful API。

## 要求

- 包括 Controller、DTO、VO、Service、ServiceImpl、Mapper。
- 使用 Spring Boot 3 + MyBatis-Plus。
- 使用统一 Result<T> 响应格式。
- 添加参数校验（@Valid、@NotBlank 等）。
- 添加接口注释和 JavaDoc。
- 接口路径遵循 RESTful 风格。
- 生成示例请求和响应 JSON。

生成的代码应可以直接复制到项目中使用。

package ai.toafrica.agrios.framework.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Jackson 全局序列化配置
 *
 * 设计原则: 输出固定漂亮格式("2026-05-25 11:16:20" 带空格),
 *           输入宽松同时接受空格和 ISO T 分隔符.
 *           前端 ElementPlus DatePicker 默认输出 ISO 带 T,要兼容它。
 */
@Configuration
public class JacksonConfig {

    /** 输出: yyyy-MM-dd HH:mm:ss (带空格,人类友好) */
    private static final DateTimeFormatter OUT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUT_DATE      = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUT_TIME      = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 输入宽松: 同时接受 "2026-05-25 11:16:20" 和 "2026-05-25T11:16:20",可选毫秒。
     * 用 DateTimeFormatterBuilder 拼,中间分隔符标成 optional T 或 optional space。
     */
    private static final DateTimeFormatter IN_DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .appendPattern("HH:mm:ss")
            .optionalStart().appendPattern(".SSS").optionalEnd()
            .toFormatter();

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsr310Customizer() {
        return builder -> builder
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(OUT_DATE_TIME))
                .serializerByType(LocalDate.class,     new LocalDateSerializer(OUT_DATE))
                .serializerByType(LocalTime.class,     new LocalTimeSerializer(OUT_TIME))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(IN_DATE_TIME))
                .deserializerByType(LocalDate.class,     new LocalDateDeserializer(OUT_DATE))
                .deserializerByType(LocalTime.class,     new LocalTimeDeserializer(OUT_TIME));
    }
}

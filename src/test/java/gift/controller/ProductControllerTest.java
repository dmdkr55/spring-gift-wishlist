package gift.controller;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.model.Product;
import gift.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll(); // 테스트마다 DB 초기화
    }

    @Test
    void 상품_등록_성공() throws Exception {
        // given
        Product product = new Product();
        product.setName("아이스 아메리카노");
        product.setPrice(2500);
        product.setImageUrl("americano.jpg");

        // when & then
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.name").value("아이스 아메리카노"))
            .andExpect(jsonPath("$.price").value(2500))
            .andExpect(jsonPath("$.imageUrl").value("americano.jpg"));
    }

    @Test
    void 상품_이름이_비어있으면_등록_실패() throws Exception {
        Product product = new Product();
        product.setPrice(2500);
        product.setImageUrl("americano.jpg");
        // name null인 상태

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0]").value("이름은 필수 입력 값입니다.")); // 예외 메시지 일치 여부
    }

    @Test
    void 상품_이름이_15자를_초과하면_등록_실패() throws Exception {
        Product product = new Product();
        product.setName("abcdefghijklmnop"); // 16자
        product.setPrice(2500);
        product.setImageUrl("americano.jpg");

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0]").value("이름은 15자까지 입력 가능합니다."));
    }

    @Test
    void 상품_이름에_허용되지_않은_특수문자가_포함되면_등록_실패() throws Exception {
        Product product = new Product();
        product.setName("콜라$"); // '$'는 허용 안됨
        product.setPrice(1500);
        product.setImageUrl("cola.jpg");

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0]").value(
                "특수 문자는 ((, ), [, ], +, -, &, /, _)만 가능합니다. 또는 '카카오'가 포함된 문구는 담당 MD와 협의가 필요합니다."));
    }

    @Test
    void 상품_이름에_카카오가_포함되면_등록_실패() throws Exception {
        Product product = new Product();
        product.setName("카카오 커피"); // '카카오'는 허용 안됨
        product.setPrice(1500);
        product.setImageUrl("kakao_coffee.jpg");

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[0]").value(
                "특수 문자는 ((, ), [, ], +, -, &, /, _)만 가능합니다. 또는 '카카오'가 포함된 문구는 담당 MD와 협의가 필요합니다."));
    }

    @Test
    void 상품_가격이_null이면_등록_실패() throws Exception {
        Product product = new Product();
        product.setName("제로 콜라");
        product.setImageUrl("zero.jpg");
        // price null 상태

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))).andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.messages[0]").value("가격은 필수 입력 값입니다.")); // 예외 메시지는 @NotNull 메시지에 따라 변경
    }

    @Test
    void 이미지_URL이_null이면_등록_실패() throws Exception {
        Product product = new Product();
        product.setName("제로 콜라");
        product.setPrice(2000);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))).andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.messages[0]").value("이미지 Url은 필수 입력 값입니다.")); // 예외 메시지는 @NotNull 메시지에 따라 변경
    }

    @Test
    void 상품_가격이_음수면_등록_실패() throws Exception {
        Product product = new Product();
        product.setName("제로 콜라");
        product.setPrice(-100);
        product.setImageUrl("zero.jpg");

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product))).andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.messages[0]").value("가격은 음수가 될 수 없습니다.")); // 예외 메시지는 @NotNull 메시지에 따라 변경
    }

    @Test
    void 전체_상품_조회() throws Exception {
        // given
        productRepository.save(new Product(null, "아이스 아메리카노", 2500, "americano.jpg"));
        productRepository.save(new Product(null, "아이스티", 3000, "iceTea.jpg"));

        // when & then
        mockMvc.perform(get("/api/products")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(2)))
            .andExpect(jsonPath("$[0].name", not(emptyString())));
    }

    @Test
    void 특정_상품_조회() throws Exception {
        // given
        Product saved = productRepository.save(
            new Product(null, "아이스 아메리카노", 2500, "americano.jpg"));

        // when & then
        mockMvc.perform(get("/api/products/" + saved.getId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("아이스 아메리카노"));
    }

    @Test
    void 상품_수정() throws Exception {
        Product saved = productRepository.save(
            new Product(null, "아이스 아메리카노", 2500, "americano.jpg"));

        Product updated = new Product();
        updated.setName("핫 아메리카노");
        updated.setPrice(2000);
        updated.setImageUrl("americano.jpg");

        mockMvc.perform(
                put("/api/products/" + saved.getId()).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updated)))
            .andExpect(status().isNoContent());
    }

    @Test
    void 상품_삭제() throws Exception {
        Product saved = productRepository.save(
            new Product(null, "아이스 아메리카노", 2500, "americano.jpg"));

        mockMvc.perform(delete("/api/products/" + saved.getId())).andExpect(status().isNoContent());
    }
}

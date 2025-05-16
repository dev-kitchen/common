package com.linkedout.common.model.entity;

import com.linkedout.common.model.type.UnitType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("recipe")
public class Recipe extends BaseEntity {
	@Id
	@Column("id")
	private Long id;

	@Column("name")
	private String name;

	@Column("main_image")
	private String mainImage;

	@Column("method")
	private String method;

	@Column("type")
	private String type;

	@Column("tip")
	private String tip;

	@Column("youtube_link")  // 유튜브 링크 컬럼
	private String youtubeLink;

	@Column("source")  // 출처 컬럼
	private String source;

	@Column("ingredients")
	private List<Ingredient> ingredients;

	@Column("sauces")
	private List<Sauces> sauces;

	@Column("manual_steps")
	private List<ManualStep> manualSteps;

	@Column("normalized_name")
	private String normalizedName;

	@Column("author_id")  // author를 author_id로 변경 (Account 테이블의 ID를 참조)
	private Long authorId;

	@Data
	public static class Ingredient {
		private String name;
		private UnitType unit;
		private String quantity;
	}

	@Data
	public static class Sauces {
		private String name;
		private UnitType unit;
		private String quantity;
	}

	@Data
	public static class ManualStep {
		private Integer id;
		private String text;
		private String image;
	}
}

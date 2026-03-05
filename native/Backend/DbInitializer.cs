using System.Linq;
using ProduceApi.Data;
using System.Collections.Generic;

namespace ProduceApi.Data
{
    public static class DbInitializer
    {
        public static void Initialize(ProduceDbContext context)
        {
            context.Database.EnsureCreated();

            // Seed Seasonal Crops
            if (!context.SeasonalCrops.Any())
            {
                var crops = new List<SeasonalCrop>
                {
                    new SeasonalCrop { CropCode = "LA1", CropName = "甘藍", Season = "春季", Description = "春季高麗菜鮮甜多汁", StartMonth = 3, EndMonth = 5 },
                    new SeasonalCrop { CropCode = "FJ1", CropName = "番茄", Season = "春季", Description = "春季番茄酸甜適中", StartMonth = 3, EndMonth = 5 },
                    new SeasonalCrop { CropCode = "T1", CropName = "西瓜", Season = "夏季", Description = "消暑解渴最佳選擇", StartMonth = 6, EndMonth = 8 },
                    new SeasonalCrop { CropCode = "P1", CropName = "木瓜", Season = "夏季", Description = "夏季木瓜香甜可口", StartMonth = 6, EndMonth = 8 },
                    new SeasonalCrop { CropCode = "F1", CropName = "柑桔", Season = "秋季", Description = "秋季柑桔富含維他命C", StartMonth = 9, EndMonth = 11 },
                    new SeasonalCrop { CropCode = "S1", CropName = "葡萄", Season = "秋季", Description = "秋季葡萄果肉飽滿", StartMonth = 9, EndMonth = 11 },
                    new SeasonalCrop { CropCode = "SA1", CropName = "蘿蔔", Season = "冬季", Description = "冬季蘿蔔賽人蔘", StartMonth = 12, EndMonth = 2 },
                    new SeasonalCrop { CropCode = "LH1", CropName = "菠菜", Season = "冬季", Description = "冬季菠菜營養豐富", StartMonth = 12, EndMonth = 2 }
                };
                context.SeasonalCrops.AddRange(crops);
            }

            // Seed Recipes
            if (!context.Recipes.Any())
            {
                var recipes = new List<Recipe>
                {
                    new Recipe { RecipeName = "番茄炒蛋", MainIngredient = "番茄", IngredientsJson = "[\"番茄\", \"雞蛋\"]", ImageUrl = "🍅", StepsJson = "[\"1. 番茄切塊\", \"2. 雞蛋打散炒熟\", \"3. 加入番茄拌炒\", \"4. 加點番茄醬與糖調味\"]" },
                    new Recipe { RecipeName = "蒜炒高麗菜", MainIngredient = "高麗菜", IngredientsJson = "[\"高麗菜\", \"蒜頭\"]", ImageUrl = "🥬", StepsJson = "[\"1. 高麗菜洗淨切片\", \"2. 蒜頭爆香\", \"3. 放入高麗菜大火快炒\", \"4. 加鹽調味即可\"]" },
                    new Recipe { RecipeName = "青江菜炒肉絲", MainIngredient = "青江菜", IngredientsJson = "[\"青江菜\", \"豬肉絲\"]", ImageUrl = "🥬", StepsJson = "[\"1. 青江菜洗淨切段\", \"2. 肉絲醃製\", \"3. 炒熟肉絲後加入青江菜\", \"4. 拌炒均勻即可\"]" },
                    new Recipe { RecipeName = "小白菜豆腐湯", MainIngredient = "小白菜", IngredientsJson = "[\"小白菜\", \"豆腐\"]", ImageUrl = "🥬", StepsJson = "[\"1. 煮滾高湯\", \"2. 放入切塊豆腐\", \"3. 加入小白菜煮熟\", \"4. 加鹽調味\"]" },
                    new Recipe { RecipeName = "洋蔥炒蛋", MainIngredient = "洋蔥", IngredientsJson = "[\"洋蔥\", \"雞蛋\"]", ImageUrl = "🧅", StepsJson = "[\"1. 洋蔥切絲\", \"2. 炒軟洋蔥\", \"3. 倒入蛋液炒熟\", \"4. 加鹽調味\"]" },
                    new Recipe { RecipeName = "紅蘿蔔炒肉絲", MainIngredient = "胡蘿蔔", IngredientsJson = "[\"胡蘿蔔\", \"豬肉絲\"]", ImageUrl = "🥕", StepsJson = "[\"1. 胡蘿蔔切絲\", \"2. 肉絲醃製\", \"3. 炒熟肉絲後加入胡蘿蔔絲\", \"4. 拌炒均勻即可\"]" },
                    new Recipe { RecipeName = "清炒花椰菜", MainIngredient = "花椰菜", IngredientsJson = "[\"花椰菜\", \"蒜頭\"]", ImageUrl = "🥦", StepsJson = "[\"1. 花椰菜切小朵洗淨\", \"2. 滾水川燙\", \"3. 蒜頭爆香\", \"4. 加入花椰菜拌炒\"]" },
                    new Recipe { RecipeName = "魚香茄子", MainIngredient = "茄子", IngredientsJson = "[\"茄子\", \"豬絞肉\"]", ImageUrl = "🍆", StepsJson = "[\"1. 茄子切段炸軟\", \"2. 炒香絞肉與辛香料\", \"3. 加入茄子與醬汁\", \"4. 悶煮入味\"]" },
                    new Recipe { RecipeName = "馬鈴薯燉肉", MainIngredient = "馬鈴薯", IngredientsJson = "[\"馬鈴薯\", \"豬肉塊\"]", ImageUrl = "🥔", StepsJson = "[\"1. 馬鈴薯與肉切塊\", \"2. 炒香肉塊\", \"3. 加入馬鈴薯與醬油、糖、水\", \"4. 燉煮至軟爛\"]" },
                    new Recipe { RecipeName = "玉米排骨湯", MainIngredient = "玉米", IngredientsJson = "[\"玉米\", \"排骨\"]", ImageUrl = "🌽", StepsJson = "[\"1. 排骨川燙去血水\", \"2. 玉米切段\", \"3. 將排骨與玉米放入鍋中\", \"4. 加水燉煮一小時，加鹽調味\"]" }
                };
                context.Recipes.AddRange(recipes);
            }

            context.SaveChanges();
        }
    }
}

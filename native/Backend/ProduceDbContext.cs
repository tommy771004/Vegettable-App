using Microsoft.EntityFrameworkCore;
using System.ComponentModel.DataAnnotations;

namespace ProduceApi.Data
{
    public class ProduceDbContext : DbContext
    {
        public ProduceDbContext(DbContextOptions<ProduceDbContext> options) : base(options) { }

        // Define tables for the backend database
        public DbSet<UserFavorite> UserFavorites { get; set; }
        public DbSet<PriceHistory> PriceHistories { get; set; }
        public DbSet<CommunityPrice> CommunityPrices { get; set; }
        public DbSet<UserStat> UserStats { get; set; }
        public DbSet<SeasonalCrop> SeasonalCrops { get; set; }
        public DbSet<Recipe> Recipes { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<UserFavorite>()
                .HasKey(uf => new { uf.UserId, uf.ProduceId });
        }
    }

    public class SeasonalCrop
    {
        [Key]
        public int Id { get; set; }
        public string CropCode { get; set; }
        public string CropName { get; set; }
        public string Season { get; set; }
        public string Description { get; set; }
        public int StartMonth { get; set; }
        public int EndMonth { get; set; }
    }

    public class Recipe
    {
        [Key]
        public int Id { get; set; }
        public string RecipeName { get; set; }
        public string MainIngredient { get; set; } // e.g., "番茄"
        public string IngredientsJson { get; set; } // Stored as JSON string
        public string ImageUrl { get; set; }
        public string StepsJson { get; set; } // Stored as JSON string
    }

    public class CommunityPrice
    {
        [Key]
        public int Id { get; set; }
        
        [Required]
        public string CropCode { get; set; }
        
        public string CropName { get; set; }
        
        public string MarketName { get; set; }
        
        public double RetailPrice { get; set; }
        
        public string UserId { get; set; }
        
        public System.DateTime ReportDate { get; set; }
    }

    public class UserFavorite
    {
        [Required]
        public string UserId { get; set; }
        
        [Required]
        public string ProduceId { get; set; }
        
        public string ProduceName { get; set; }
        
        public double TargetPrice { get; set; }
    }

    public class PriceHistory
    {
        [Key]
        public int Id { get; set; }
        
        [Required]
        public string ProduceId { get; set; }
        
        public string ProduceName { get; set; }
        
        public string MarketCode { get; set; }
        
        public double AveragePrice { get; set; }
        
        public System.DateTime RecordDate { get; set; }
    }

    public class UserStat
    {
        [Key]
        public string UserId { get; set; }
        
        public int ContributionPoints { get; set; }
        
        public string Level { get; set; }
        
        public string FcmToken { get; set; }
    }
}

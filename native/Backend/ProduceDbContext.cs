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

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<UserFavorite>()
                .HasKey(uf => new { uf.UserId, uf.ProduceId });
        }
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

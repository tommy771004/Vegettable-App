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

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<UserFavorite>()
                .HasKey(uf => new { uf.UserId, uf.ProduceId });
        }
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
        
        public string MarketCode { get; set; }
        
        public double AveragePrice { get; set; }
        
        public System.DateTime RecordDate { get; set; }
    }
}

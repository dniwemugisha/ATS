# 🚀 Production Deployment Guide

## AI Model Configuration for Production

### 📋 **Available AI Models**

| Model | Size | Use Case | Performance | Quality |
|-------|------|----------|-------------|---------|
| `phi3` | 2.3GB | Development, Fast prototyping | ⚡⚡⚡ | ⭐⭐⭐ |
| `llama3` | 4.7GB | Production, High quality | ⚡⚡ | ⭐⭐⭐⭐⭐ |
| `llama3.1` | 4.7GB | Latest production model | ⚡⚡ | ⭐⭐⭐⭐⭐ |
| `codellama` | 3.8GB | Code analysis, Tech roles | ⚡⚡⚡ | ⭐⭐⭐⭐ |
| `mistral` | 4.1GB | Balanced performance | ⚡⚡ | ⭐⭐⭐⭐ |

## 🔧 **Environment Configuration**

### **1. Development Environment (.env.development)**
```bash
# Development - Fast startup, smaller model
OLLAMA_MODEL=phi3
OLLAMA_AUTO_PULL=true
OLLAMA_STARTUP_TIMEOUT=30
```

### **2. Production Environment (.env.production)**
```bash
# Production - High quality analysis
OLLAMA_MODEL=llama3
OLLAMA_AUTO_PULL=true
OLLAMA_STARTUP_TIMEOUT=60
```

### **3. Tech-Focused Environment (.env.tech)**
```bash
# Tech roles - Code-optimized model
OLLAMA_MODEL=codellama
OLLAMA_AUTO_PULL=true
OLLAMA_STARTUP_TIMEOUT=45
```

### **4. Custom Model Environment (.env.custom)**
```bash
# Custom model configuration
OLLAMA_MODEL=mistral
OLLAMA_AUTO_PULL=false  # Pre-pulled manually
OLLAMA_STARTUP_TIMEOUT=30
```

## 🚀 **Deployment Steps**

### **Step 1: Choose Your Environment**

```bash
# Copy the appropriate environment file
cp .env.production .env

# OR create custom configuration
cat > .env << 'EOF'
# Database configuration
POSTGRES_DB=ats_database
POSTGRES_USER=ats_user
POSTGRES_PASSWORD=your_secure_password

# Backend configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://ats-database:5432/ats_database
JWT_SECRET=your_jwt_secret_min_32_chars_for_production

# AI Model configuration
OLLAMA_MODEL=llama3
OLLAMA_AUTO_PULL=true
OLLAMA_STARTUP_TIMEOUT=60

# Production settings
APP_RESUME_ANALYSIS_PROVIDER=ollama
EOF
```

### **Step 2: Deploy with Your Chosen Model**

```bash
# Single command deployment
docker-compose up -d --build

# Monitor startup (especially first time)
docker-compose logs -f ollama
```

### **Step 3: Verify Model Download**

```bash
# Check model status
docker exec ats-ollama ollama list

# Test model
docker exec ats-ollama ollama run llama3 "Test message"
```

## 🔄 **Switching Models in Production**

### **Method 1: Environment Variable Update**
```bash
# Update .env file
echo "OLLAMA_MODEL=llama3.1" >> .env

# Restart Ollama service only
docker-compose restart ollama

# Monitor new model download
docker-compose logs -f ollama
```

### **Method 2: Manual Model Management**
```bash
# Pre-download models without restart
docker exec ats-ollama ollama pull llama3.1
docker exec ats-ollama ollama pull codellama

# Update environment and restart
echo "OLLAMA_MODEL=llama3.1" >> .env
docker-compose restart ollama
```

### **Method 3: Multi-Model Setup**
```bash
# Download multiple models
docker exec ats-ollama ollama pull phi3
docker exec ats-ollama ollama pull llama3
docker exec ats-ollama ollama pull codellama

# Quick switching by updating .env
echo "OLLAMA_MODEL=codellama" > .env
docker-compose restart ollama
```

## 📊 **Production Considerations**

### **Hardware Requirements**

| Model | RAM | Storage | CPU | Recommended Use |
|-------|-----|---------|-----|-----------------|
| phi3 | 4GB+ | 3GB | 2+ cores | Development/Testing |
| llama3 | 8GB+ | 6GB | 4+ cores | Production |
| codellama | 6GB+ | 5GB | 4+ cores | Tech Recruitment |
| mistral | 6GB+ | 5GB | 4+ cores | General Production |

### **Performance Tuning**

```bash
# GPU Support (if available)
# Uncomment in docker-compose.yml:
deploy:
  resources:
    reservations:
      devices:
        - driver: nvidia
          count: 1
          capabilities: [gpu]

# Increase timeout for slower systems
OLLAMA_STARTUP_TIMEOUT=120

# Disable auto-pull for faster startups
OLLAMA_AUTO_PULL=false
```

### **Monitoring & Health Checks**

```bash
# Health check endpoint
curl http://localhost:11434/api/tags

# Model-specific health check
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model":"llama3","prompt":"Health check","stream":false}'

# Container health
docker-compose ps
docker stats ats-ollama
```

## 🛡️ **Security for Production**

### **1. Secure Environment Variables**
```bash
# Use secure password generation
JWT_SECRET=$(openssl rand -base64 32)
POSTGRES_PASSWORD=$(openssl rand -base64 16)

# Store in secure .env file with restricted permissions
chmod 600 .env
```

### **2. Network Security**
```bash
# Restrict Ollama access (internal only)
# Remove from docker-compose.yml:
# ports:
#   - "11434:11434"  # Comment out for internal access only
```

### **3. Resource Limits**
```yaml
# Add to docker-compose.yml
deploy:
  resources:
    limits:
      memory: 8G
      cpus: '4.0'
```

## 📈 **Scaling Considerations**

### **Horizontal Scaling**
```bash
# Multiple Ollama instances
docker-compose scale ollama=3

# Load balancer configuration needed
# (nginx, traefik, etc.)
```

### **Model Optimization**
```bash
# Use quantized models for better performance
OLLAMA_MODEL=llama3:8b-instruct-q4_0  # Specific quantization
OLLAMA_MODEL=phi3:mini               # Smaller variant
```

## 🔧 **Troubleshooting**

### **Common Issues**

1. **Model Download Timeout**
   ```bash
   # Increase timeout
   OLLAMA_STARTUP_TIMEOUT=300
   
   # Or pre-download manually
   docker exec ats-ollama ollama pull llama3
   ```

2. **Insufficient Memory**
   ```bash
   # Check memory usage
   docker stats ats-ollama
   
   # Use smaller model
   OLLAMA_MODEL=phi3
   ```

3. **Slow Performance**
   ```bash
   # Enable GPU support
   # Check hardware requirements
   # Consider model quantization
   ```

### **Logs & Debugging**
```bash
# Ollama service logs
docker logs ats-ollama

# Backend AI integration logs
docker logs ats-backend | grep -i ollama

# Full system logs
docker-compose logs
```

## 🚀 **Quick Production Setup Examples**

### **Small Team (< 100 resumes/day)**
```bash
echo "OLLAMA_MODEL=phi3" > .env
docker-compose up -d
```

### **Medium Company (< 1000 resumes/day)**
```bash
echo "OLLAMA_MODEL=llama3" > .env
docker-compose up -d
```

### **Enterprise (> 1000 resumes/day)**
```bash
echo "OLLAMA_MODEL=llama3.1" > .env
# Add GPU support and resource limits
docker-compose up -d
```

### **Tech Company (Code-heavy roles)**
```bash
echo "OLLAMA_MODEL=codellama" > .env
docker-compose up -d
``` 
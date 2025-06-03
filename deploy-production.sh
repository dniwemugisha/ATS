#!/bin/bash

# ATS System - Production Deployment Script
# This script helps deploy the ATS system with different AI model configurations

set -e

echo "🚀 ATS System - Production Deployment"
echo "======================================"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_status "Docker is running"
}

# Function to show available deployment options
show_deployment_options() {
    echo ""
    echo "📋 Available Deployment Configurations:"
    echo ""
    echo "1. 🏃 Development (phi3 - 2.3GB, fast startup)"
    echo "2. 🏢 Production (llama3 - 4.7GB, high quality)"
    echo "3. 💻 Tech-Focused (codellama - 3.8GB, code analysis)"
    echo "4. ⚖️  Balanced (mistral - 4.1GB, good performance)"
    echo "5. 🆕 Latest (llama3.1 - 4.7GB, newest model)"
    echo "6. 🔧 Custom (specify your own model)"
    echo ""
}

# Function to get user choice
get_deployment_choice() {
    while true; do
        read -p "Select deployment option (1-6): " choice
        case $choice in
            1) DEPLOYMENT_TYPE="development"; OLLAMA_MODEL="phi3"; break;;
            2) DEPLOYMENT_TYPE="production"; OLLAMA_MODEL="llama3"; break;;
            3) DEPLOYMENT_TYPE="tech"; OLLAMA_MODEL="codellama"; break;;
            4) DEPLOYMENT_TYPE="balanced"; OLLAMA_MODEL="mistral"; break;;
            5) DEPLOYMENT_TYPE="latest"; OLLAMA_MODEL="llama3.1"; break;;
            6) DEPLOYMENT_TYPE="custom"; get_custom_model; break;;
            *) print_warning "Please select a valid option (1-6)";;
        esac
    done
}

# Function to get custom model
get_custom_model() {
    echo ""
    print_info "Enter custom model name (e.g., llama3, phi3, codellama, mistral, etc.)"
    read -p "Model name: " OLLAMA_MODEL
    if [[ -z "$OLLAMA_MODEL" ]]; then
        print_error "Model name cannot be empty"
        get_custom_model
    fi
}

# Function to setup environment file
setup_environment() {
    print_info "Setting up environment configuration..."
    
    # Check if .env already exists
    if [[ -f ".env" ]]; then
        print_warning ".env file already exists"
        read -p "Do you want to backup and replace it? (y/N): " replace_env
        if [[ $replace_env =~ ^[Yy]$ ]]; then
            mv .env .env.backup.$(date +%Y%m%d_%H%M%S)
            print_status "Existing .env backed up"
        else
            print_info "Using existing .env file"
            return
        fi
    fi
    
    # Create .env file based on deployment type
    case $DEPLOYMENT_TYPE in
        "development")
            if [[ -f "env.development.example" ]]; then
                cp env.development.example .env
            else
                create_default_env
            fi
            ;;
        "production")
            if [[ -f "env.production.example" ]]; then
                cp env.production.example .env
            else
                create_default_env
            fi
            ;;
        "tech")
            if [[ -f "env.tech.example" ]]; then
                cp env.tech.example .env
            else
                create_default_env
            fi
            ;;
        *)
            create_default_env
            ;;
    esac
    
    # Update OLLAMA_MODEL in .env
    if [[ -f ".env" ]]; then
        sed -i.bak "s/OLLAMA_MODEL=.*/OLLAMA_MODEL=$OLLAMA_MODEL/" .env
        rm .env.bak
    fi
    
    print_status "Environment configured for $DEPLOYMENT_TYPE deployment with $OLLAMA_MODEL model"
}

# Function to create default .env file
create_default_env() {
    cat > .env << EOF
# ATS System - $DEPLOYMENT_TYPE Configuration
POSTGRES_DB=ats_database
POSTGRES_USER=ats_user
POSTGRES_PASSWORD=ats_secure_password_$(date +%s)

SPRING_DATASOURCE_URL=jdbc:postgresql://ats-database:5432/ats_database
JWT_SECRET=$(openssl rand -base64 32 2>/dev/null || echo "your_jwt_secret_must_be_at_least_32_characters_long")
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin@atsafrica
SPRING_SECURITY_USER_ROLES=ADMIN

REACT_APP_API_URL=http://localhost:8080

# AI Model Configuration
OLLAMA_MODEL=$OLLAMA_MODEL
OLLAMA_AUTO_PULL=true
OLLAMA_STARTUP_TIMEOUT=60
APP_RESUME_ANALYSIS_PROVIDER=ollama

# Email Configuration (Optional)
#MAIL_USERNAME=your_email@company.com
#MAIL_PASSWORD=your_email_password

# OAuth Configuration (Optional)
#LINKEDIN_CLIENT_ID=your_linkedin_client_id
#LINKEDIN_CLIENT_SECRET=your_linkedin_client_secret
EOF
}

# Function to show model information
show_model_info() {
    echo ""
    print_info "Selected AI Model: $OLLAMA_MODEL"
    
    case $OLLAMA_MODEL in
        "phi3")
            echo "  📦 Size: 2.3GB"
            echo "  ⚡ Performance: Fast"
            echo "  🎯 Best for: Development, prototyping"
            echo "  💾 RAM needed: 4GB+"
            ;;
        "llama3")
            echo "  📦 Size: 4.7GB"
            echo "  ⚡ Performance: High quality"
            echo "  🎯 Best for: Production environments"
            echo "  💾 RAM needed: 8GB+"
            ;;
        "codellama")
            echo "  📦 Size: 3.8GB"
            echo "  ⚡ Performance: Code-optimized"
            echo "  🎯 Best for: Tech recruitment"
            echo "  💾 RAM needed: 6GB+"
            ;;
        "mistral")
            echo "  📦 Size: 4.1GB"
            echo "  ⚡ Performance: Balanced"
            echo "  🎯 Best for: General production"
            echo "  💾 RAM needed: 6GB+"
            ;;
        "llama3.1")
            echo "  📦 Size: 4.7GB"
            echo "  ⚡ Performance: Latest & best"
            echo "  🎯 Best for: Cutting-edge production"
            echo "  💾 RAM needed: 8GB+"
            ;;
        *)
            echo "  📦 Size: Unknown"
            echo "  ⚡ Performance: Custom model"
            echo "  🎯 Best for: Specific use case"
            ;;
    esac
    echo ""
}

# Function to deploy the system
deploy_system() {
    print_info "Starting deployment..."
    
    echo ""
    print_warning "This may take several minutes on first run (model download)"
    read -p "Continue with deployment? (Y/n): " continue_deploy
    
    if [[ $continue_deploy =~ ^[Nn]$ ]]; then
        print_info "Deployment cancelled"
        exit 0
    fi
    
    # Stop any existing containers
    print_info "Stopping existing containers..."
    docker-compose down 2>/dev/null || true
    
    # Start deployment
    print_info "Building and starting services..."
    docker-compose up -d --build
    
    # Monitor Ollama startup
    print_info "Monitoring AI model setup..."
    echo "📥 Waiting for $OLLAMA_MODEL model download to complete..."
    
    # Wait for Ollama container to be ready
    timeout=300
    counter=0
    while [[ $counter -lt $timeout ]]; do
        if docker logs ats-ollama 2>/dev/null | grep -q "Ollama setup complete"; then
            print_status "AI model setup completed successfully!"
            break
        fi
        if docker logs ats-ollama 2>/dev/null | grep -q "failed to start"; then
            print_error "AI model setup failed"
            break
        fi
        echo -n "."
        sleep 5
        counter=$((counter + 5))
    done
    echo ""
    
    if [[ $counter -ge $timeout ]]; then
        print_warning "Timeout waiting for AI model setup. Check logs: docker logs ats-ollama"
    fi
}

# Function to show final status
show_final_status() {
    echo ""
    echo "🎉 Deployment Summary"
    echo "===================="
    
    # Check container status
    if docker-compose ps | grep -q "Up"; then
        print_status "Services are running"
        
        echo ""
        echo "🌐 Access URLs:"
        echo "  Frontend: http://localhost:3001"
        echo "  Backend API: http://localhost:8080"
        echo "  API Documentation: http://localhost:8080/swagger-ui.html"
        echo "  AI Service: http://localhost:11434"
        
        echo ""
        echo "👤 Default Admin Account:"
        echo "  Email: admin@ats.istafrica"
        echo "  Password: admin@atsafrica"
        
        echo ""
        echo "🔧 Management Commands:"
        echo "  View logs: docker-compose logs -f"
        echo "  Stop services: docker-compose down"
        echo "  Restart services: docker-compose restart"
        echo "  Check AI models: docker exec ats-ollama ollama list"
        
    else
        print_error "Some services failed to start"
        echo "Check logs with: docker-compose logs"
    fi
    
    echo ""
    print_info "Deployment configuration saved in .env file"
    print_info "See docs/PRODUCTION_DEPLOYMENT.md for detailed management guide"
}

# Main execution
main() {
    check_docker
    show_deployment_options
    get_deployment_choice
    show_model_info
    setup_environment
    deploy_system
    show_final_status
}

# Run main function
main "$@" 
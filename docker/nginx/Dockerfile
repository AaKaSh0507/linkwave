FROM nginx:1.25-alpine

LABEL maintainer="LinkWave Team" \
    version="1.0" \
    description="LinkWave Nginx Reverse Proxy"

# Remove default config
RUN rm -rf /etc/nginx/conf.d/*

# Copy custom configuration
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf /etc/nginx/conf.d/default.conf

# Create cache directory
RUN mkdir -p /var/cache/nginx && \
    chown -R nginx:nginx /var/cache/nginx

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/nginx-health || exit 1

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]

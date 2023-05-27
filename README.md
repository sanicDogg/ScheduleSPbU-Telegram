```shell
docker build . -t schedule-spbu
```

```shell
docker run --name bot-spbu -d --restart unless-stopped schedule-spbu
``` 
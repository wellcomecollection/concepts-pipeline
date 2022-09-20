
awk '{ln=length}
       ln>max{delete result; max=ln}
       ln==max{result[NR]=$0}
       END{for(i in result) print result[i] }'

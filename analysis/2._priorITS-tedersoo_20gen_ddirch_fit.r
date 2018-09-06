#Fit dirlichet models to functional groups of fungi from Tedersoo et al. Temperate Latitude Fungi.
#No hierarchy required, as everything is observed at the site level. Each observation is a unique site.
#Missing data are allowed.
#clear environment
rm(list = ls())
library(data.table)
library(doParallel)
source('paths.r')
source('NEFI_functions/ddirch_site.level_JAGS.r')
source('NEFI_functions/ddirch_site.level_JAGS_int.only.r')
source('NEFI_functions/crib_fun.r')

#detect and register cores.
n.cores <- detectCores()
registerDoParallel(cores=n.cores)

#load tedersoo data.
#d <- data.table(readRDS(tedersoo_ITS.prior_for_analysis.path)) #old analysis path.
d <- data.table(readRDS(tedersoo_ITS.prior_fromSV_analysis.path))
#d <- d[1:35,] #for testing
start <- which(colnames(d)=="Mortierella")
  end <- which(colnames(d)=="Amphinema"  )
y <- d[,start:end]
x <- d[,.(pC,cn,pH,NPP,map,mat,forest,conifer,relEM)]
d <- cbind(y,x)
d <- d[complete.cases(d),] #optional. This works with missing data.
y <- d[,colnames(d) %in% colnames(y), with = F]
x <- d[,colnames(d) %in% colnames(x), with = F]

#make other column
y$other <- 1 - rowSums(y)
y <- data.frame(lapply(y,crib_fun, N = ncol(y)*nrow(y)))
#in the rare case where one column actually needs to be a zero for a row to prevent to summing over 1...
for(i in 1:nrow(y)){
  if(rowSums(y[i,]) > 1){
    y[i,] <- y[i,] / rowSums(y[i,])
  }
}
y <- as.data.frame(y)

#Drop in intercept, setup predictor matrix.
intercept <- rep(1, nrow(x))
x <- cbind(intercept, x)

#IMPORTANT: LOG TRANSFORM MAP.
#log transform map, magnitudes in 100s-1000s break JAGS code.
x$map <- log(x$map)

#define multiple subsets
x.clim <- x[,.(intercept,NPP,mat,map)]
x.site <- x[,.(intercept,pC,cn,pH,forest,conifer,relEM)]
x.all  <- x[,.(intercept,pC,cn,pH,NPP,mat,map,forest,conifer,relEM)]
x.list <- list(x.clim,x.site,x.all)

#fit model using function.
#This take a long time to run, probably because there is so much going on.
#fit <- site.level_dirlichet_jags(y=y,x_mu=x,adapt = 50, burnin = 50, sample = 100)
#for running production fit on remote.
output.list<-
  foreach(i = 1:length(x.list)) %dopar% {
    fit <- site.level_dirlichet_jags(y=y,x_mu=x.list[i],adapt = 200, burnin = 1000, sample = 1000, parallel = T)
    return(fit)
  }

#get intercept only fit.
output.list[[length(x.list) + 1]] <- site.level_dirlichet_intercept.only_jags(y=y, silent.jags = T)

#name the items in the list
names(output.list) <- c('climate.preds','site.preds','all.preds','int.only')

cat('Saving fit...\n')
saveRDS(output.list, ted_ITS.prior_fg_JAGSfit)
cat('Script complete. \n')

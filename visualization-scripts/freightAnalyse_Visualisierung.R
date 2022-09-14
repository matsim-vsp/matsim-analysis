#packages
require(tidyverse)
require(viridis)
require(treemap)
require(ggridges)

#file paths data
filepath_freightVehicleStats <- "C:/Users/J/Documents/VSP_Berlin/Kai/Johanna/Johanna/FreightAnalyse/Base_21/freightVehicleStats.tsv"

#read data
data_freightVehicleStats <- read_tsv(filepath_freightVehicleStats)

#prepare data for better plotting:
#set order of groups in plot via mutate & fct_relevel
#permit colouring of different vehicleType groups by creating new columns with mutate &case_when

data_VehicleStats_prepped <- data_freightVehicleStats %>%
  arrange(travelDistance) %>% 
  mutate(ID = row_number()) %>% 
  mutate(vehicleType = fct_relevel(vehicleType, "light8t", "light8t_frozen", "light8t_frozen_electro",
                                   "medium18t", "medium18t_electro",
                                   "heavy26t", "heavy26t_frozen", "heavy26t_electro", "heavy40t") ) %>%
  mutate(weight = case_when(vehicleType == "light8t"~ "8t",
                            vehicleType == "light8t_frozen"~ "8t",
                            vehicleType == "light8t_frozen_electro" ~ "8t",
                            vehicleType == "medium18t"~"18t",
                            vehicleType == "medium18t_electro" ~"18t",
                            vehicleType == "heavy26t"~"26t",
                            vehicleType == "heavy26t_frozen"~"26t",
                            vehicleType == "heavy26t_electro" ~"26t",
                            vehicleType == "heavy40t"  ~"40t"),
         veh_type = case_when(vehicleType == "light8t"~ "standard",
                              vehicleType == "light8t_frozen"~ "frozen",
                              vehicleType == "light8t_frozen_electro" ~ "frozen_electro",
                              vehicleType == "medium18t"~"standard",
                              vehicleType == "medium18t_electro" ~"electro",
                              vehicleType == "heavy26t"~"standard",
                              vehicleType == "heavy26t_frozen"~"frozen",
                              vehicleType == "heavy26t_electro" ~"electro",
                              vehicleType == "heavy40t"  ~"standard"))

----------
#boxplot with jitter

data_VehicleStats_prepped %>% 
 ggplot(aes(x = vehicleType, y=travelDistance, fill= weight))+
  geom_boxplot()+
  scale_fill_viridis(discrete = TRUE, alpha = 0.6)+
  geom_jitter(size = 3, aes(shape = veh_type, alpha = 0.6))+
  scale_shape_manual(values = c(15,16,17,18))+
  scale_y_continuous(labels = function(x) format(x, scientific = FALSE))+
  theme(axis.text.x = element_text(angle = 45, hjust = 1))+
  xlab("vehicle type")+
  ylab("travel distance")+
  labs(fill= "weight",
       shape = "vehicle type")+
  guides(alpha = "none")

#violinplot with jitter
#scale: if "area" (default), all violins have the same area (before trimming the tails). 
        #If "count", areas are scaled proportionally to the number of observations. 
        #If "width", all violins have the same maximum width.

data_freightVehicleStats %>%
  ggplot(aes(x = vehicleType, y=travelDistance, fill= vehicleType))+
  geom_violin(scale = "width")+
  scale_fill_viridis(discrete = TRUE, alpha = 0.6)+
  geom_jitter(color = "black", size =2, alpha = 0.8)+
  scale_y_continuous(labels = function(x) format(x, scientific = FALSE))+
  theme(axis.text.x = element_text(angle = 45, hjust = 1))+
  xlab("vehicle type")+
  ylab("travel distance")+
  labs(fill= "vehicle type")

#ridges
#adjust vertical overlap between groups with scale- scale = 1 results in them just touching
data_VehicleStats_prepped %>%
  mutate(weight = fct_relevel(weight, "8t","18t","26t","40t") ) %>%
  ggplot(aes(x = travelDistance, y = weight, fill = veh_type, alpha = 0.3))+
           geom_density_ridges(scale = 1.2)+
           theme_ridges()+
  scale_fill_viridis(discrete = TRUE)+
  scale_x_continuous(labels = function(x) format(x, scientific = FALSE))+
  xlab("travel distance (m)")+
  ylab("vehicle weight")+
  labs(fill= "vehicle type")+
  guides(alpha = "none")


#grouped points

data_VehicleStats_prepped%>%
  mutate(weight = fct_relevel(weight, "8t","18t","26t","40t") ) %>%
  ggplot(aes(x= ID, y = travelDistance))+
  geom_jitter(aes(colour= veh_type))+
  facet_grid(.~weight)+
  scale_y_continuous(labels = function(x) format(x, scientific = FALSE))+
  theme(axis.text.x = element_blank(),
        axis.ticks = element_blank())+
  xlab("vehicles")+
  ylab("travel distance (m)")+
  labs(colour= "vehicle types")

        